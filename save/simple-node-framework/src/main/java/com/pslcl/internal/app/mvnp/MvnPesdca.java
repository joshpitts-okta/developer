package com.pslcl.internal.app.mvnp;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pslcl.chad.app.swing.FrameBase;
import com.pslcl.chad.app.swing.FrameBase.CloseListener;
import com.pslcl.internal.app.mvnp.model.MvnP;
import com.pslcl.internal.app.mvnp.view.NotDoneYetPanel;
import com.pslcl.internal.app.mvnp.view.ProjectsPanel;

@SuppressWarnings("javadoc")
public final class MvnPesdca extends FrameBase implements CloseListener
{
    private static final long serialVersionUID = -6130574745797065985L;
    public static MvnPesdca mvnp;
    public static final String logDir = "log";
    public static final String dataDir = "data";
    public static final String dataFile = "projects.json";

    private static final String applicationTitle = "Pesdca Maven Build Helper";
    public static final String mvnpPrefixKey = "com.panasonic.mvnp";
    public static final String mvnpPreferencesNode = "com/panasonic/mvnp";

    private final String home;
    private final Logger log;
    private final ProjectsControl projectsControl;
    private MvnP mvnpData;

    public MvnPesdca()
    {
        super(applicationTitle, mvnpPreferencesNode);
        mvnp = this;
        home = System.getProperty("user.home").replace('\\', '/') + "/mvnp/";
        File file = new File(home + logDir);
        boolean createdDirs = file.mkdirs();
        boolean logDirExists = file.exists();
        if (!logDirExists)
        {
            System.err.println("unable to create/access logging folder: " + logDir);
            System.exit(1);
        }
        file = new File(home + dataDir);
        file.mkdirs();

        System.setProperty("log-file-base-name", logDir + "/mvnp");
        log = LoggerFactory.getLogger(getClass());
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusPrinter.print(lc);
        log.info("file logging to: " + logDir + "/mvnp.log");
        if (createdDirs)
            log.info("first time creation of data directories: " + logDir);
        loadJson(home, dataDir, dataFile);
        projectsControl = new ProjectsControl(this);
        addCloseListener(this);
    }

    public MvnP getMvnpData()
    {
        return mvnpData;
    }
    
    @Override
    public void setBusy(boolean busy)
    {
        projectsControl.setBusy(busy);
    }

    @Override
    public void refresh()
    {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                Runnable runner = new Runnable(){@Override public void run()
                {
                    projectsControl.refresh();
                    MvnPesdca.this.contentPane.revalidate();
                    MvnPesdca.this.contentPane.repaint();
                }};
                SwingUtilities.invokeLater(runner);
                return null;
            }
        };
        worker.execute();
    }
    
    public ProjectsControl getProjectsControl()
    {
        return projectsControl;
    }

    @SuppressWarnings("unused")
    @Override
    public JMenuBar createMenu()
    {
        JMenuBar menuBar = new JMenuBar();
        toolBar = new JToolBar();
        toolBar.setRollover(true);

        // file Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(FileMenuMnemonic);
        new ImportAction(fileMenu);
        new ExportAction(fileMenu);
        new ClearUserPreferencesAction(fileMenu, this);
        fileMenu.addSeparator();
        new ExitAction(fileMenu);
        menuBar.add(fileMenu);

        projectsControl.addMenu(menuBar, toolBar);
        return menuBar;
    }

    /*
        File
            Clear Preferences
            Exit
        Projects
            Add
            Delete
            Build
            Environment
    */
    // menu bar
    public static int FileMenuMnemonic = KeyEvent.VK_F;
    public static int ProjectsMenuMnemonic = KeyEvent.VK_P;

    // Mnemonics
    // File menu items
    public static int ClearPreferencesActionMnemonic = KeyEvent.VK_C; // in FrameBase - this will not change it
    public static int ExportActionMnemonic = KeyEvent.VK_E;
    public static int ImportActionMnemonic = KeyEvent.VK_I;
    public static int ExitActionMnemonic = KeyEvent.VK_X; // in FrameBase, this will not change it

    // Projects menu bar items
    public static int AddProjectActionMnemonic = KeyEvent.VK_A;
    public static int DeleteProjectActionMnemonic = KeyEvent.VK_D;
    public static int BuildProjectActionMnemonic = KeyEvent.VK_B;
    public static int EnvProjectActionMnemonic = KeyEvent.VK_E;
    

    // Accelerators
    public static int ExitActionAccelerator = KeyEvent.VK_F4; // in FrameBase, this will not change it
    public static int AddProjectActionAccelerator = KeyEvent.VK_A;
    public static int DeleteProjectActionAccelerator = KeyEvent.VK_D;
    public static int BuildProjectActionAccelerator = KeyEvent.VK_B;
    public static int EnvProjectActionAccelerator = KeyEvent.VK_E;
    
    private void nextStartupPhase()
    {
        try
        {
            nextStartupPhase(new Dimension(660, 460));
        } catch (Exception e)
        {
            displayException(log, this, "GUI initialization failed", "", e);
        }
    }

    @Override
    protected void initGui()
    {
        ProjectsPanel panel = projectsControl.getProjectsPanel();
//        panel.setMercenaryComboBoxModel(mercs.getMercsComboBoxModel(), mercs.getLastSelectedMercenary());
//        if(mercs.getLastSelectedMercenary() == null)
//            setBusy(true);
        swapContentPane(panel);
        super.initGui();
    }

    @Override
    public void close()
    {
        projectsControl.close();
        saveJson(home, dataDir, dataFile);
        System.exit(0);
    }

    static String readFile(String path, Charset encoding) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }
    
    private synchronized void loadJson(String home, String dataDir, String dataFile)
    {
        byte[] encoded;
        Path path = Paths.get(home, dataDir, dataFile);
        File file = path.toFile();
        if (file.exists())
        {
            String jsonString = null;
            try
            {
                encoded = Files.readAllBytes(path);
                jsonString = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
            } catch (IOException e)
            {
                FrameBase.displayException(log, mvnp, "Failed to load: " + path.toString(), "", e);
            }
            log.info(jsonString);
            Gson gson = new GsonBuilder().create();
            mvnpData = gson.fromJson(jsonString, MvnP.class);
        }
        if(mvnpData == null)
            mvnpData = new MvnP(true);
    }

    protected synchronized void saveJson(String home, String dataDir, String dataFile)
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonString = gson.toJson(mvnpData);

        Path path = Paths.get(home, dataDir, dataFile);
        OutputStreamWriter osw = null;
        try
        {
            FileOutputStream fos = new FileOutputStream(path.toFile());
            osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            osw.write(jsonString);
        } catch (IOException e)
        {
            FrameBase.displayException(log, mvnp, "Failed to save: " + path.toString(), "", e);
        } finally
        {
            if (osw != null)
            {
                try
                {
                    osw.close();
                } catch (IOException e)
                {
                    FrameBase.displayException(log, mvnp, "Failed to close: " + path.toString(), "", e);
                }
            }
        }
    }
    
    final class ImportAction extends AbstractAction
    {
        private static final long serialVersionUID = -1002287733146179911L;
        public final String Image = "exit.gif";
        public static final String Label = "Import...";
        public static final String MouseOver = "Import Mercenaries";
        public final int Mnemonic = MvnPesdca.ImportActionMnemonic;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        public ImportAction(JMenu menu)
        {
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);

            menu.add(this);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            swapContentPane(new NotDoneYetPanel("ImportAction"));
        }
    }

    final class ExportAction extends AbstractAction
    {
        private static final long serialVersionUID = 3697274705505180630L;
        public final String Image = "exit.gif";
        public static final String Label = "Export...";
        public static final String MouseOver = "Export Mercenaries";
        public final int Mnemonic = MvnPesdca.ExportActionMnemonic;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        public ExportAction(JMenu menu)
        {
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);

            menu.add(this);
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            swapContentPane(new NotDoneYetPanel("ExportAction"));
        }
    }

    public static void main(String[] args)
    {
        try
        {
            new MvnPesdca().nextStartupPhase();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
