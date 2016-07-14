package com.pslcl.chad.app.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;

@SuppressWarnings("javadoc")
public abstract class FrameBase extends JFrame 
{
    private static final long serialVersionUID = -910946500218073411L;

    private final FramePreference framePreference;

    private final String applicationTitle;
    private final AtomicBoolean shutdown;
    private Dimension initialSize;
    protected Container contentPane;
    
    private JLabel leftMessageLabel;
    private JLabel rightMessageLabel;
    private String lastLeftMessage;
    private String lastRightMessage;
    
    private final Vector<CloseListener> closeListeners;
    private final String preferencesFrameKey;
    
    protected Preferences userRoot;
    protected final AtomicInteger startupPhase;
    protected JPanel currentPanel;
    protected JToolBar toolBar;
    
    public FrameBase(String title, String preferencesRoot)
    {
        this(title, Preferences.userRoot().node(preferencesRoot), FramePreference.MainFrameLocation);
    }
    
    public FrameBase(String title, Preferences preferences, String frameKey)
    {
        preferencesFrameKey = frameKey;
        applicationTitle = title;
        shutdown = new AtomicBoolean();
        closeListeners = new Vector<CloseListener>();
        userRoot = preferences;
        framePreference = new FramePreference(userRoot, frameKey);
        startupPhase = new AtomicInteger();
    }

    public Preferences getUserRoot()
    {
        return userRoot;
    }

    public void setUserRoot(Preferences root)
    {
        userRoot = root;
    }

    public abstract void setBusy(boolean busy);
    
    public void addCloseListener(CloseListener listener)
    {
        closeListeners.add(listener);
    }

    public void removeCloseListener(CloseListener listener)
    {
        closeListeners.remove(listener);
    }

    private void initFrame()
    {
        logWarningIfThreadIsNotAwt("initFrame");
        if(framePreference.name.equals(FramePreference.NotSetYet))
        {
            setSize(initialSize.width, initialSize.height);
            setLocationRelativeTo(null);
        }else
        {
            setSize(framePreference.width, framePreference.height);
            setLocation(framePreference.x, framePreference.y);
        }
        setTitle(applicationTitle);

        String os = System.getProperty("os.name").toLowerCase();
        if(os != null && os.contains("windows"))
        {
            try
            {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
            } catch (Exception e)
            {
                displayException(null, this, "Look and Feel Error", "failed to initialize look and feel", e);
            }
        }

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        setJMenuBar(createMenu());
        contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(buildMessageLine(), BorderLayout.SOUTH);

        setVisible(true);
    }

    protected void initGui()
    {
        logWarningIfThreadIsNotAwt("initGui");
        contentPane.validate();
        contentPane.repaint();
        refresh();
    }

    @SuppressWarnings("unused")
    protected JMenuBar createMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        toolBar = new JToolBar();
        toolBar.setRollover(true);

        // file Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_I);
        new ExitAction(fileMenu);
        new ClearUserPreferencesAction(fileMenu, this);
        
        menuBar.add(fileMenu);

        return menuBar;
    }

    public static Component findComponent(Point p, Component top) 
    {
        Component c = null;
        if (top.isShowing()) 
        {
          if (top instanceof RootPaneContainer)
            c = ((RootPaneContainer) top).getLayeredPane().findComponentAt(
                SwingUtilities.convertPoint(top, p, ((RootPaneContainer) top).getLayeredPane()));
          else
            c = ((Container) top).findComponentAt(p);
        }
        return c;
      }

    public static void logWarningIfThreadIsNotAwt(String message)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            if(message == null)
                message = "Not an actual exception: Current Thread is NOT the AWT Event Thread";
            else
                message = "Not an actual exception: " + message;
            Thread currentThread = Thread.currentThread();
            Exception e = new Exception(message);
            e.setStackTrace(currentThread.getStackTrace());
            System.out.println(message);
            e.printStackTrace();
        }
    }
    
    private JPanel buildMessageLine()
    {
        // Add a message panel with a left and a right message label
        JPanel messagePanel = new JPanel(false);
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.X_AXIS));
        leftMessageLabel = new JLabel();
        rightMessageLabel = new JLabel();

        // setLeftMessage("left message" /*controller.getState().toString()*/);
        // setRightMessage("right message");

        messagePanel.add(Box.createRigidArea(new Dimension(5, 0)));
        messagePanel.add(leftMessageLabel);
        messagePanel.add(Box.createHorizontalGlue());
        messagePanel.add(Box.createRigidArea(new Dimension(15, 0)));
        messagePanel.add(rightMessageLabel);
        messagePanel.add(Box.createRigidArea(new Dimension(5, 0)));
        return messagePanel;
    }

    @SuppressWarnings("static-method")
    public void swapAndSetFocus(final SwappedFocusListener sfl, final JComponent component)
    {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                Runnable runner = new Runnable(){@Override public void run()
                {
                    component.requestFocus();
                    sfl.focusRequested(component);
                }};
                SwingUtilities.invokeLater(runner);
                return null;
            }
        };
        worker.execute();
    }
    
    protected void nextStartupPhase(Dimension initSize) throws Exception
    {
        this.initialSize = initSize;
        // some things, like requestFocus require that the gui has actually been
        // drawn/displayed before it can successfully complete. This method allows
        // for normal runtime threads to intersperse various startup phases.

        final Runnable nextPhase = new Runnable()
        {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run()
            {
                switch(startupPhase.get())
                {
                    case 0: // constructor complete, do init frame
                        startupPhase.incrementAndGet();
                        initFrame();
                    break;
                    case 1: // init frame complete, do initgui
                        startupPhase.incrementAndGet();
                        initGui();
                    break;
                    default:
                        break;
                }
            }
        };
        if(SwingUtilities.isEventDispatchThread())
            nextPhase.run();
        else
            SwingUtilities.invokeAndWait(nextPhase);
        if(startupPhase.get() < 2)
            nextStartupPhase(initSize);
    }
    
    public void setLeftMessage(String message)
    {
        lastLeftMessage = message;
        leftMessageLabel.setText(message);
    }

    public void setRightMessage(String message)
    {
        lastRightMessage = message;
        rightMessageLabel.setText(message);
    }

    public void pushLeftMessage(String message)
    {
        leftMessageLabel.setText(message);
    }

    public void pushRightMessage(String message)
    {
        rightMessageLabel.setText(message);
    }

    public void popLeftMessage()
    {
        leftMessageLabel.setText(lastLeftMessage);
    }

    public void popRightMessage()
    {
        rightMessageLabel.setText(lastRightMessage);
    }

    public void swapContentPane(JPanel panel)
    {
        if(currentPanel != null)
        {
            contentPane.remove(currentPanel);
            currentPanel = null;
        }
        currentPanel = panel;
        contentPane.add(currentPanel, BorderLayout.CENTER);
        contentPane.revalidate();
        contentPane.repaint();
    }
    
    @SuppressWarnings("unused")
    @Override
    public void dispose()
    {
        if(shutdown.get())
            return;
        shutdown.set(true);
        Preferences node = userRoot.node(preferencesFrameKey);
        new FramePreference(FramePreference.MainFrameLocation, getLocation(), getSize(), node);
        int size = closeListeners.size();
        for(int i=0; i < size; i++)
            closeListeners.get(i).close();
        super.dispose();
    }
    
    public final class ExitAction extends AbstractAction
    {
        private static final long serialVersionUID = -890738937965375500L;
        public final String Image = "exit.gif";
        public static final String Label = "Exit";
        public static final String MouseOver = "Exit the application";
        public static final int Mnemonic = KeyEvent.VK_X;
        public static final int Accelerator = KeyEvent.VK_F4;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        public ExitAction(JMenu menu)
        {
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);
            // putValue(SMALL_ICON, AvailableTreeCellRenderer.getIcon(Image));

            JMenuItem item = menu.add(this);
            item.setAccelerator(KeyStroke.getKeyStroke(Accelerator, AcceleratorMask));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            dispose();
        }
    }
   
    public static final class ClearUserPreferencesAction extends AbstractAction
    {
        private static final long serialVersionUID = -2703927379593234181L;
        public final String Image = "clearPreferences.gif";
        public static final String Label = "Clear Preferences";
        public static final String MouseOver = "Clear all User preferences";
        public static final int Mnemonic = KeyEvent.VK_C;
        public static final int Accelerator = KeyEvent.VK_C;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        private final FrameBase frame;
        
        public ClearUserPreferencesAction(JMenu menu, FrameBase frame)
        {
            this.frame = frame;
            
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);
            // putValue(SMALL_ICON, AvailableTreeCellRenderer.getIcon(Image));

//            JMenuItem item = 
            menu.add(this);
//            item.setAccelerator(KeyStroke.getKeyStroke(Accelerator, AcceleratorMask));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            new ClearPreferencesTask(frame).start();
        }
    }
     
    protected void refresh()
    {
        contentPane.validate();
        contentPane.repaint();
    }
    
    final static class ClearPreferencesTask extends Thread
    {
        private final FrameBase frame;
        private final Preferences userRoot;
        private final String rootName;
        
        public ClearPreferencesTask(FrameBase frame)
        {
            this.frame = frame;
            userRoot = frame.getUserRoot();
            rootName = userRoot.name();
        }
        
        @Override
        public void run()
        {
            frame.pushLeftMessage("Clearing user preferences ... please wait");
            try
            {
                String[] names = userRoot.childrenNames();
                for(int i = 0; i < names.length; i++)
                {
                    Preferences node = userRoot.node(names[i]);
                    node.removeNode();
                    node.flush();
                }
                userRoot.removeNode();
                userRoot.flush();
                frame.setUserRoot(Preferences.userRoot().node(rootName));
            }catch(BackingStoreException e)
            {
                displayException(null, frame, null, "Failed to remove preferences", e);
            }
            frame.popLeftMessage();
        }
    }
    
    
    public static JButton addButtonToPanel(String label, boolean enabled, String toolTip, JPanel panel)
    {
        return addButtonToPanel(label, enabled, toolTip, panel, null);
    }

    public static JButton addButtonToPanel(String label, boolean enabled, String toolTip, JPanel panel, ActionListener listener)
    {
        JButton button = new JButton(label);
        if (listener != null)
            button.addActionListener(listener);
        button.setEnabled(enabled);
        button.setToolTipText(toolTip);
        panel.add(button);
        return button;
    }

    public static JCheckBox addCheckBoxToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel)
    {
        return addCheckBoxToPanel(label, value, enabled, width, toolTip, panel, null, null, null);
    }

    public static JCheckBox addCheckBoxToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel, ActionListener listener)
    {
        return addCheckBoxToPanel(label, value, enabled, width, toolTip, panel, null, listener, null);
    }
    
    public static JCheckBox addCheckBoxToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel, ActionListener alistener, MouseListener mlistener)
    {
        return addCheckBoxToPanel(label, value, enabled, width, toolTip, panel, null, alistener, mlistener);
    }
    
    public static JCheckBox addCheckBoxToPanel(
            JLabel label, boolean value, boolean enabled, int width,  
            String toolTip, JPanel panel, String layoutConstraint, ActionListener alistener, MouseListener mlistener)
    {
        JCheckBox cbox = new JCheckBox();
        cbox.setToolTipText(toolTip);
        cbox.setSelected(value);
        if (alistener != null)
            cbox.addActionListener(alistener);
        if (!enabled)
            cbox.setEnabled(false);
        addTextParamToPanel(label, cbox, width, -1, toolTip, panel, layoutConstraint, null, mlistener);
        return cbox;
    }

    public static JRadioButton addRadioButtonToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel)
    {
        return addRadioButtonToPanel(label, value, enabled, width, toolTip, panel, null, null, null);
    }

    public static JRadioButton addRadioButtonToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel, ActionListener listener)
    {
        return addRadioButtonToPanel(label, value, enabled, width, toolTip, panel, null, listener, null);
    }
    
    public static JRadioButton addRadioButtonToPanel(JLabel label, boolean value, boolean enabled, int width, String toolTip, JPanel panel, ActionListener alistener, MouseListener mlistener)
    {
        return addRadioButtonToPanel(label, value, enabled, width, toolTip, panel, null, alistener, mlistener);
    }
    
    public static JRadioButton addRadioButtonToPanel(
            JLabel label, boolean value, boolean enabled, int width,  
            String toolTip, JPanel panel, String layoutConstraint, 
            ActionListener alistener, MouseListener mlistener)
    {
        JRadioButton radio = new JRadioButton();
        radio.setToolTipText(toolTip);
        radio.setSelected(value);
        if (alistener != null)
            radio.addActionListener(alistener);
        if (!enabled)
            radio.setEnabled(false);
        addTextParamToPanel(label, radio, width, -1, toolTip, panel, layoutConstraint, null, mlistener);
        return radio;
    }

    public static JPanel addTextParamToPanel(JLabel label, JComponent field, int width, int gap, String toolTip, JPanel container)
    {
        return addTextParamToPanel(label, field, width, gap, toolTip, container, null, null);
    }

    public static JPanel addTextParamToPanel(JLabel label, JComponent field, int width, int gap, String toolTip, JPanel container, DocumentListener docListener)
    {
        return addTextParamToPanel(label, field, width, gap, toolTip, container, null, docListener, null);
    }
    
    public static JPanel addTextParamToPanel(JLabel label, JComponent field, int width, int gap, String toolTip, JPanel container, DocumentListener docListener, MouseListener mouseListener)
    {
        return addTextParamToPanel(label, field, width, gap, toolTip, container, null, docListener, mouseListener);
    }
    
    public static JPanel addTextParamToPanel(
            JLabel label, JComponent field, int width, int gap, 
            String toolTip, JPanel container, String layoutConstraint, 
            DocumentListener docListener, MouseListener mouseListener)
    {
        label.setForeground(Color.black);
        label.setPreferredSize(new Dimension(width, (int) label.getPreferredSize().getHeight()));
        if (field instanceof JTextField)
            ((JTextField) field).getDocument().addDocumentListener(docListener);
        if (field instanceof JTextArea)
            ((JTextArea) field).getDocument().addDocumentListener(docListener);
        field.setToolTipText(toolTip);
        label.setToolTipText(toolTip);
        if(mouseListener != null)
        {
            field.addMouseListener(mouseListener);
            label.addMouseListener(mouseListener);
        }
        JPanel paramPanel = new JPanel(new BorderLayout(gap == -1 ? 5 : width, 0));
        paramPanel.add(label, BorderLayout.WEST);
        paramPanel.add(field, BorderLayout.CENTER);
        if (container != null)
        {
            if(layoutConstraint != null)
                container.add(paramPanel, layoutConstraint);
            else
                container.add(paramPanel);
        }
        if(mouseListener != null)
            paramPanel.addMouseListener(mouseListener);
        return paramPanel;
    }

    public static Point getLocation(Component component)
    {
        Point totalRelativeLocation = new Point();
        do
        {
            Point relativeLocation = component.getLocation();
            totalRelativeLocation.x += relativeLocation.x;
            totalRelativeLocation.y += relativeLocation.y;
            component = component.getParent();
        } while (component != null);
        return totalRelativeLocation;
    }
    
    public static JLabel getLabel(String value, AtomicInteger columnWidth)
    {
        JLabel label = new JLabel(value, SwingConstants.RIGHT);
        int width = label.getPreferredSize().width;
        columnWidth.set(Math.max(columnWidth.get(), width));
        return label;
    }

    
    public static void displayException(Logger logger, Component parentComponent, String title, String message, Throwable t)
    {
        StringWriter output = new StringWriter();
        if(message != null)
        {
            output.write(message);
            output.write("\n");
            if(t != null)
            {
                String tmsg = t.getMessage();
                if(tmsg != null)
                    output.write(tmsg+"\n");
            }
        }
        if(title == null)
            title = "Unexpected Exception";
        PrintWriter pw = new PrintWriter(output);
        if(t != null)
            t.printStackTrace(pw);
        if(logger != null)
            logger.error(message, t);
        JOptionPane.showMessageDialog(parentComponent, output.toString(), title, JOptionPane.ERROR_MESSAGE);
    }
    
    public static final class FramePreference
    {
        public static final String NotSetYet = "NotSetYet";
        public static final String MainFrameLocation = "mainFrameLocation";

        public static final String NameKey = "name";
        public static final String XKey = "x";
        public static final String YKey = "y";
        public static final String WidthKey = "width";
        public static final String HeightKey = "height";
        
        public static final String ServerDetail = "ServerDetail";
        public static final String ProducerDetail = "ProducerDetail";
        public static final String ParserDetail = "ParserDetail";
        public static final String AuthProducerDetail = "AuthProducerDetail";
        public static final String UserDetail = "UserDetail";
        public static final String AuthUserDetail = "AuthUserDetail";
        
        public static final String ActiveAtClose = "activeAtClose";
        public String name;
        public int x;
        public int y;
        public int width;
        public int height;
        public boolean activeAtClose;
        public boolean parentValues;
        private Preferences rootNode;
        private String frameNodeRootRelativelName;
        private Preferences frameNode;
        private Preferences detailNode;
        private Preferences parentDetailNode;
        private String parentName;
        
        public FramePreference()
        {
        }
        
        public FramePreference(Preferences root, String rootRelativeName)
        {
            rootNode = root;
            frameNodeRootRelativelName = rootRelativeName;
            frameNode = root.node(rootRelativeName);
            name = frameNode.get(NameKey, NotSetYet);
            x = frameNode.getInt(XKey, -1);
            y = frameNode.getInt(YKey, -1);
            width = frameNode.getInt(WidthKey, -1);
            height = frameNode.getInt(HeightKey, -1);
        }
        
        public FramePreference(Preferences node, boolean clear)
        {
            if(!clear)
            {
                name = node.get(NameKey, NotSetYet);
                x = node.getInt(XKey, -1);
                y = node.getInt(YKey, -1);
                width = node.getInt(WidthKey, -1);
                height = node.getInt(HeightKey, -1);
            }else
            {
                name = NotSetYet;
                x = -1;
                y = -1;
                width = -1;
                height = -1;
                node.put(NameKey, NotSetYet);
                node.putInt(XKey, -1);
                node.putInt(YKey, -1);
                node.putInt(WidthKey, -1);
                node.putInt(HeightKey, -1);
                persist(node);
            }
            frameNode = node;
        }
        
        public FramePreference(String name, Point location, Dimension size, Preferences node)
        {
            this.name = name;
            x = location.x;
            y = location.y;
            width = size.width;
            height = size.height;
            persist(node);
        }
        
        public FramePreference(String name, int locationX, int locationY, int sizeX, int sizeY, Preferences node)
        {
            this.name = name;
            x = locationX;
            y = locationY;
            width = sizeX;
            height = sizeY;
            persist(node);
        }
        
        public void setFrame(Point location, Dimension size) throws BackingStoreException
        {
            x = location.x;
            y = location.y;
            width = size.width;
            height = size.height;
            if(frameNode == null || !rootNode.nodeExists(frameNodeRootRelativelName))
                frameNode = rootNode.node(frameNodeRootRelativelName);
            persist(frameNode);
        }
        
        public void setDetail(Point location, Dimension size)
        {
            x = location.x;
            y = location.y;
            width = size.width;
            height = size.height;
            if(detailNode != null)
                persist(detailNode);
            if(parentDetailNode != null)
                persist(parentDetailNode, true);
        }
        
        private void persist(Preferences node)
        {
            persist(node, false);
        }
        
        private void persist(Preferences node, boolean parent)
        {
            if(parent)
                node.put(NameKey, parentName);
            else
                node.put(NameKey, name);
            node.putInt(XKey, x);
            node.putInt(YKey, y);
            node.putInt(WidthKey, width);
            node.putInt(HeightKey, height);
            try
            {
                node.flush();
            }
            catch (BackingStoreException e)
            {
                throw new RuntimeException("Failed to flush frame preferences", e);
            }
        }
    }
    
    @SuppressWarnings("unused")
    public static void clearPreferences(Preferences node) throws BackingStoreException
    {
        String[] typeNames;
        typeNames = node.childrenNames();
        for(int i=0; i < typeNames.length; i++)
        {
            // check this level for different dialog types .. server, producer, user etc
            Preferences typeNode = node.node(typeNames[i]);
            String[] selectorNames = typeNode.childrenNames();
            for(int j=0; j < selectorNames.length; j++)
            {
                Preferences selectorNode = typeNode.node(selectorNames[j]);
                new FramePreference(selectorNode, true);
            }
            new FramePreference(typeNode, true);
        }
    }
    
    public interface CloseListener
    {
        public void close();
    }
    
    public static abstract class DialogBase extends JDialog  implements WindowListener
    {
        private static final long serialVersionUID = 3491805146724476460L;
        protected final FramePreference framePreference;
        
        public DialogBase(Frame frame, FramePreference preferences, String title, boolean modal)
        {
            super(frame, title, modal);
            framePreference = preferences;
            addWindowListener(this); 
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        }
        
        protected void setToFramePreferences(int defaultWidth, int defaultHeight, Component defaultComp, String name)
        {
            if(framePreference == null || framePreference.name.equals(FramePreference.NotSetYet))
            {
                setSize(defaultWidth, defaultHeight);
                setLocationRelativeTo(defaultComp);
            }else
            {
                setSize(framePreference.width, framePreference.height);
                setLocation(framePreference.x, framePreference.y);
            }
            if(framePreference != null)
                framePreference.name = name;
        }
        
        protected void saveToPreferences()
        {
            if(framePreference != null)
                try{
                    Dimension psize = getSize();
                    framePreference.setFrame(getLocation(), psize);} catch (BackingStoreException e1){/*best try*/}
        }
        
        @Override public void windowClosed(WindowEvent e){saveToPreferences();}
        @Override public void windowActivated(WindowEvent e){/* do nothing */}
        @Override public void windowClosing(WindowEvent e){/* do nothing */}
        @Override public void windowDeactivated(WindowEvent e){/* do nothing */}
        @Override public void windowDeiconified(WindowEvent e){/* do nothing */}
        @Override public void windowIconified(WindowEvent e){/* do nothing */}
        @Override public void windowOpened(WindowEvent e){/* do nothing */}
        
        public void refresh()
        {
            /* do nothing */
        }
    }
}
