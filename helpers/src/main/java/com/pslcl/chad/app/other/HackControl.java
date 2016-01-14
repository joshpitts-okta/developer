package com.pslcl.chad.app.other;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.other.view.HackPanel;

@SuppressWarnings("javadoc")
public class HackControl
{
    private final Hack mvnp;

//    private AddProjectAction addProjectAction;
    private DeleteProjectAction deleteProjectAction;
//    private BuildProjectAction buildProjectAction;
//    private EnvProjectAction envProjectAction;
    
    private JMenuItem addProjectMenuItem;
    private JMenuItem deleteProjectMenuItem;
    private JMenuItem buildProjectMenuItem;
    private JMenuItem envProjectMenuItem;

    private final Logger log;
    private HackPanel hackPanel;
    private ProjectsPopUpMenu projectsPopupMenu;

    public HackControl(Hack duster)
    {
        log = LoggerFactory.getLogger(getClass());
        this.mvnp = duster;
//        projects = mvnp.getMvnpData().getProjects();
    }

    public void refresh()
    {
//        hackPanel.refresh();
    }

    public HackPanel getHackPanel()
    {
        if(hackPanel == null)
            hackPanel = new HackPanel(mvnp, false);
        return hackPanel;
    }
    
    public ProjectsPopUpMenu getProjectsPopupMenu()
    {
        if(projectsPopupMenu == null)
            projectsPopupMenu = new ProjectsPopUpMenu();
        return projectsPopupMenu;
    }

    public void addMenu(JMenuBar menuBar, JToolBar toolBar)
    {
//        JMenu projectsMenu = new JMenu("Projects");
//        projectsMenu.setMnemonic(Hack.ProjectsMenuMnemonic);
//        addProjectAction = new AddProjectAction(projectsMenu);
//        deleteProjectAction = new DeleteProjectAction(projectsMenu);
//        buildProjectAction = new BuildProjectAction(projectsMenu);
//        envProjectAction = new EnvProjectAction(projectsMenu);
//        addProjectMenuItem = new JMenuItem(addProjectAction);
//        deleteProjectMenuItem = new JMenuItem(deleteProjectAction);
//        buildProjectMenuItem = new JMenuItem(buildProjectAction);
//        envProjectMenuItem = new JMenuItem(envProjectAction);
//        menuBar.add(projectsMenu);
    }

    public void close()
    {
    }

    public void setBusy(boolean busy)
    {
//        addProjectAction.setEnabled(!busy);
//        deleteProjectAction.setEnabled(!busy);
//        buildProjectAction.setEnabled(!busy);
//        envProjectAction.setEnabled(!busy);
    }

    final class DeleteProjectAction extends AbstractAction
    {
        public final String Image = "exit.gif";
        public static final String Label = "Delete";
        public static final String MouseOver = "Delete currently selected Project";
        public final int Mnemonic = Hack.DeleteProjectActionMnemonic;
        public final int Accelerator = Hack.DeleteProjectActionAccelerator;
        public static final int AcceleratorMask = ActionEvent.ALT_MASK;

        public DeleteProjectAction(JMenu menu)
        {
            putValue(NAME, Label);
            putValue(ACTION_COMMAND_KEY, Label);
            putValue(MNEMONIC_KEY, new Integer(Mnemonic));
            putValue(SHORT_DESCRIPTION, MouseOver);

            JMenuItem item = menu.add(this);
            item.setAccelerator(KeyStroke.getKeyStroke(Accelerator, AcceleratorMask));
            setEnabled(true);
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
//            mvnp.swapContentPane(getProjectsPanel());
//            Project project = hackPanel.getSelectedProject();
//            if(project == null)
//            {
//                Toolkit.getDefaultToolkit().beep();
//                return;
//            }
//            if(JOptionPane.showConfirmDialog(mvnp, "Delete Project " + project.toString(), "Delete Project", JOptionPane.YES_NO_OPTION) != JOptionPane.OK_OPTION)
//                return;
//            projects.deleteProject(project);
//            DefaultListModel<Project> listModel = projects.getProjectListModel();
//            hackPanel.setProjectListModel(listModel, listModel.getElementAt(0));
        }
    }

    public class ProjectsPopUpMenu extends JPopupMenu implements MouseListener
    {
        public ProjectsPopUpMenu()
        {
            add(addProjectMenuItem);
            add(deleteProjectMenuItem);
            add(buildProjectMenuItem);
            add(envProjectMenuItem);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            maybeShow(e);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            maybeShow(e);
        }
        
        private void maybeShow(MouseEvent e)
        {
            if (e.isPopupTrigger())
            {
                show(e.getComponent(), e.getX(), e.getY());
//                mainPanel.repaint();
            }
        }

        // @formatter:off
        @Override public void mouseClicked(MouseEvent e){/* nothing to do here*/}
        @Override public void mouseEntered(MouseEvent e){/* nothing to do here*/}
        @Override public void mouseExited(MouseEvent e) {/* nothing to do here*/}
        // @formatter:on
    }
}
