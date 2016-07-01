package com.pslcl.internal.test.simpleNodeFramework;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.daemon.Daemon;
import org.apache.commons.daemon.DaemonController;
import org.apache.commons.daemon.support.DaemonLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("javadoc")
public class GenericServiceRunner extends Thread
{
    private final AtomicBoolean goingDown;
    private final AtomicBoolean down;
    private final Logger log;
    public Daemon service;
    private final DaemonLoader.Context context;
    
    public GenericServiceRunner(String[] args, Daemon service)
    {
        log = LoggerFactory.getLogger(GenericServiceRunner.class);
        this.service = service;
        goingDown = new AtomicBoolean(false);
        down = new AtomicBoolean(false);
        context = new DaemonLoader.Context();
        context.setArguments(args);
    }
    
    @Override
    public void run()
    {
        try
        {
            log.info("Starting service " + service.getClass().toString());
            context.setController(new Controller(service));
            service.init(context);
            service.start();
            while(!goingDown.get())
            {
                Thread.sleep(250);
            }
            context.getController().shutdown();
            synchronized (down)
            {
                down.set(true);
                down.notifyAll();
            }
        }catch(Exception e)
        {
            log.error("failed: ", e); 
        }
    }
    
    public void close()
    {
        goingDown.set(true);
        synchronized (down)
        {
            while(!down.get())
            {
                try
                {
                    down.wait();
                } catch (InterruptedException e)
                {
                    log.warn("unexpected wakeup", e);
                    e.printStackTrace();
                }
            }
        }
    }
    
    private static class Controller implements DaemonController
    {

        Logger logger = LoggerFactory.getLogger(getClass());

        private final Daemon service;

        public Controller(Daemon service)
        {
            this.service = service;
        }

        @Override
        public void fail() throws IllegalStateException
        {
            fail("");
        }

        @Override
        public void fail(String str) throws IllegalStateException
        {
            System.err.println("Service Failed: " + str);
            System.exit(1);
        }

        @Override
        public void fail(Exception e) throws IllegalStateException
        {
            logger.debug("Failed: " + e.getMessage(), e);
            fail(e.getMessage(), e);
        }

        @Override
        public void fail(String str, Exception e) throws IllegalStateException
        {
            StringBuilder sb = new StringBuilder(str);
            Throwable t = e;
            logger.debug("Failed: " + e.getMessage(), e);
            do
            {
                String info = t.getMessage();
                if (info == null)
                    info = t.toString();
                sb.append(" (").append(info).append(")");
                t = t.getCause();
            } while (t != null);
            fail(sb.toString()); // concludes by exiting the system
        }

        @Override
        public void reload() throws IllegalStateException
        {
            try
            {
                this.service.stop();
            } catch (Exception e)
            {
                System.err.println("Stop failure (" + e.getMessage() + ")");
            }
            try
            {
                this.service.start();
            } catch (Exception e)
            {
                fail("Start failure", e);
            }
        }

        @Override
        public void shutdown() throws IllegalStateException
        {
            try
            {
                this.service.stop();
            } catch (Exception e)
            {
                fail("Stop failure", e);
            }
            this.service.destroy();
        }

    }

}