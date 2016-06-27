package com.sasg.tools.repository.interfaces.cli.cmds;

import org.opendof.tools.repository.interfaces.cli.InterfaceRepositoryManage;
import org.opendof.tools.repository.interfaces.cli.ManageController;
import org.opendof.tools.repository.interfaces.cli.cmds.group.AddGroupCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.group.DeleteGroupCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.group.GroupCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.group.ListGroupCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.group.UpdateGroupCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.iface.AddIfaceCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.iface.DeleteIfaceCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.iface.IfaceCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.iface.ListIfaceCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.iface.UpdateIfaceCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.submitter.AddSubmitterCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.submitter.DeleteSubmitterCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.submitter.SubmitterCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.submitter.UpdateSubmitterCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.subrepo.AddSubRepoCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.subrepo.DeleteSubRepoCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.subrepo.ListSubRepoCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.subrepo.SubRepoCommand;
import org.opendof.tools.repository.interfaces.cli.cmds.subrepo.UpdateSubRepoCommand;
import org.opendof.tools.repository.interfaces.core.AuthenticatedUser;
import org.opendof.tools.repository.interfaces.core.CoreController;
import org.opendof.tools.repository.interfaces.da.DataAccessor;

import com.sasg.tools.repository.interfaces.cli.cmds.migrate.AllocMigrateCommand;
import com.sasg.tools.repository.interfaces.cli.cmds.migrate.MigrateCommand;
import com.sasg.tools.repository.interfaces.cli.cmds.migrate.ScriptMigrateCommand;

@SuppressWarnings("javadoc")
public class V1toV2CustomIRManage extends InterfaceRepositoryManage
{
    public V1toV2CustomIRManage(String[] args)
    {
        super(args, true);
        MigrateCommand migrateCmd = new MigrateCommand(this, "migrate");
        addCommand(migrateCmd);
        migrateCmd.addChild(new ScriptMigrateCommand(this, migrateCmd, ScriptMigrateCommand.Command.Script.command));
        migrateCmd.addChild(new AllocMigrateCommand(this, migrateCmd, AllocMigrateCommand.Command.Alloc.command));
        command = validateCommands();
    }
    
    @Override
    public void run()
    {
        try
        {
            coreController = new CoreController();
            coreController.init(properties);
            ManageController manage = new ManageController(coreController);
            MigrateController migrateManage = new MigrateController(coreController);
            String cmdName = command.getCommandName(); 
            if(command instanceof IfaceCommand)
            {
                if(cmdName.equals(IfaceCommand.Command.Add.command))
                    manage.ifaceAdd(((AddIfaceCommand)command).getAddCmdData());
                if(cmdName.equals(IfaceCommand.Command.Delete.command))
                    manage.ifaceDelete(((DeleteIfaceCommand)command).getDeleteCmdData());
                if(cmdName.equals(IfaceCommand.Command.List.command))
                    manage.ifaceList(((ListIfaceCommand)command).getListCmdData());
                if(cmdName.equals(IfaceCommand.Command.Update.command))
                    manage.ifaceUpdate(((UpdateIfaceCommand)command).getUpdateCmdData());
                if(cmdName.equals(IfaceCommand.Command.Sync.command))
                    manage.ifaceSync(new AuthenticatedUser(DataAccessor.CliAdminName, DataAccessor.CliAdminEmail, null, null));
            }else
            if(command instanceof SubmitterCommand)
            {
                if(cmdName.equals(SubmitterCommand.Command.Add.command))
                    manage.submitterAdd(((AddSubmitterCommand)command).getAddCmdData());
                if(cmdName.equals(SubmitterCommand.Command.Delete.command))
                    manage.submitterDelete(((DeleteSubmitterCommand)command).getDeleteCmdData());
                if(cmdName.equals(SubmitterCommand.Command.List.command))
                    manage.submitterList();
                if(cmdName.equals(SubmitterCommand.Command.Update.command))
                    manage.submitterUpdate(((UpdateSubmitterCommand)command).getUpdateCmdData());
            }else
            if(command instanceof GroupCommand)
            {
                if(cmdName.equals(GroupCommand.Command.Add.command))
                    manage.groupAdd(((AddGroupCommand)command).getAddCmdData());
                if(cmdName.equals(GroupCommand.Command.Delete.command))
                    manage.groupDelete(((DeleteGroupCommand)command).getDeleteCmdData());
                if(cmdName.equals(GroupCommand.Command.List.command))
                    manage.groupList(((ListGroupCommand)command).getListCmdData());
                if(cmdName.equals(GroupCommand.Command.Update.command))
                    manage.groupUpdate(((UpdateGroupCommand)command).getUpdateCmdData());
            }else
            if(command instanceof SubRepoCommand)
            {
                if(cmdName.equals(SubRepoCommand.Command.Add.command))
                    manage.subrepoAdd(((AddSubRepoCommand)command).getAddCmdData());
                if(cmdName.equals(SubRepoCommand.Command.Delete.command))
                    manage.subrepoDelete(((DeleteSubRepoCommand)command).getDeleteCmdData());
                if(cmdName.equals(SubRepoCommand.Command.List.command))
                    manage.subrepoList(((ListSubRepoCommand)command).getListCmdData());
                if(cmdName.equals(SubRepoCommand.Command.Update.command))
                    manage.subrepoUpdate(((UpdateSubRepoCommand)command).getUpdateCmdData());
            }else
            if(command instanceof MigrateCommand)
            {
                if(cmdName.equals(MigrateCommand.Command.Script.command))
                    migrateManage.migrateScript(((ScriptMigrateCommand)command).getScriptCmdData());
                if(cmdName.equals(MigrateCommand.Command.Alloc.command))
                    migrateManage.migrateAlloc(((AllocMigrateCommand)command).getAllocCmdData());
            }
            log.debug(getClass().getSimpleName() + " exiting OK");
            close(0);
        } catch (Throwable t)
        {
            if (coreController != null)
                coreController.destroy();
            log.error("failed: ", t);
            close(ApplicationError);
        }
    }

    @Override
    public void close(int ccode)
    {
        if (coreController != null)
            coreController.destroy();
        super.close(ccode);
    }

    public enum Command
    {
        Interface("interface"), 
        Submitter("submitter"), 
        Group("group"), 
        SubRepo("subrepo"), 
        Migrate("migrate");
        
        Command(String command)
        {
            this.command = command;
        }
        
        public static Command getCommand(String arg)
        {
            if(arg.equals(Interface.command))
                return Interface;
            if(arg.equals(Group.command))
                return Group;
            if(arg.equals(SubRepo.command))
                return SubRepo;
            if(arg.equals(Migrate.command))
                return Migrate;
            if(arg.equals(Submitter.command))
                return Submitter;
            return null;
        }
        public String command;
    }
    
    public static void main(String args[])
    {
        new V1toV2CustomIRManage(args).start();
    }
}