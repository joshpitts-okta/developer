package com.pslcl.chad.app.other;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import com.pslcl.chad.app.other.view.HackPanel;
import com.pslcl.chad.app.swing.FrameBase;
import com.pslcl.chad.app.swing.FrameBase.CloseListener;

@SuppressWarnings({ "serial", "javadoc" })
public final class Hack extends FrameBase implements CloseListener
{
    public static Hack hack;
    public static final String logDir = "log";
    public static final String dataDir = "data";
    public static final String dataFile = "projects.json";

    private static final String applicationTitle = "Hack";
    public static final String mvnpPrefixKey = "com.pslcl.app.hack";
    public static final String mvnpPreferencesNode = "com/pslcl/app/hack";

    private final String home;
    private final Logger log;
    private final HackControl hackControl;

    public Hack()
    {
        super(applicationTitle, mvnpPreferencesNode);
        hack = this;
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
//        loadJson(home, dataDir, dataFile);
        hackControl = new HackControl(this);
        addCloseListener(this);
    }

    @Override
    public void setBusy(boolean busy)
    {
        hackControl.setBusy(busy);
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
                    hackControl.refresh();
                    Hack.this.contentPane.revalidate();
                    Hack.this.contentPane.repaint();
                }};
                SwingUtilities.invokeLater(runner);
                return null;
            }
        };
        worker.execute();
    }
    
    public HackControl getHackControl()
    {
        return hackControl;
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
        new ClearUserPreferencesAction(fileMenu, this);
        fileMenu.addSeparator();
        new ExitAction(fileMenu);
        menuBar.add(fileMenu);

        hackControl.addMenu(menuBar, toolBar);
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
        HackPanel panel = hackControl.getHackPanel();
//        panel.setMercenaryComboBoxModel(mercs.getMercsComboBoxModel(), mercs.getLastSelectedMercenary());
//        if(mercs.getLastSelectedMercenary() == null)
//            setBusy(true);
        swapContentPane(panel);
        super.initGui();
    }

    @Override
    public void close()
    {
        hackControl.close();
//        saveJson(home, dataDir, dataFile);
        System.exit(0);
    }

    static String readFile(String path, Charset encoding) throws IOException
    {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }
    
    public static void main(String[] args)
    {
        try
        {
            new Hack().nextStartupPhase();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
