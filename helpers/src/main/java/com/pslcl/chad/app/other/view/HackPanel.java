package com.pslcl.chad.app.other.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pslcl.chad.app.other.Hack;
import com.pslcl.chad.app.swing.FrameBase;

@SuppressWarnings("javadoc")
public class HackPanel extends JPanel implements DocumentListener, ActionListener
{
    private static final int MaxPossible = 10;
    //    public static final String packageVersionKey = "PACKAGE_VERSION:";
    //    public static final String emitBuildRepositoryKey = "EMIT_BUILD_REPOSITORY:";
    //    public static final String emitBuildRevisionkey = "EMIT_BUILD_REVISION:";
    //    public static final String emitBuildPathKey = "EMIT_BUILD_PATH:";
    //    public static final String emitBuildBuildNumKey = "EMIT_BUILD_BUILDNUM:";
    //    public static final String emitBuildDateKey = "EMIT_BUILD_DATE:";

    private final int numberOfColumns = 10;
    private final int numberOfHitColumns = 3;

    private final Hack mvnp;
    private final Logger log;
    private final AtomicBoolean initDone;

    private JTextField[] givenFields;
    private final JTextField[] possibleFields;
    private final JTextField[] hitFields;
    private final JButton clearButton;
    private final Model[] models;


    public HackPanel(Hack mvnp, boolean editable)
    {
        log = LoggerFactory.getLogger(getClass());
        FrameBase.logWarningIfThreadIsNotAwt(getClass().getName() + ".constructor");
        this.mvnp = mvnp;
        initDone = new AtomicBoolean(false);
        
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        JPanel hackPanel = new JPanel();
        hackPanel.setLayout(new BoxLayout(hackPanel, BoxLayout.Y_AXIS));
        hackPanel.setBorder(new TitledBorder("Hack Strings"));
        
        AtomicInteger maxWidth = new AtomicInteger();
        JLabel[] stringLabels = new JLabel[MaxPossible];
        for(int i=0; i < stringLabels.length; i++)
            stringLabels[i] = FrameBase.getLabel("String "+i + ":", maxWidth);
        int stringWidth = maxWidth.get();
        
        maxWidth = new AtomicInteger();
        JLabel[] hitLabels = new JLabel[MaxPossible];
        for(int i=0; i < hitLabels.length; i++)
            hitLabels[i] = FrameBase.getLabel("hits "+i + ":", maxWidth);
        int hitWidth = maxWidth.get();
        
        maxWidth = new AtomicInteger();
        JLabel[] possibleLabels = new JLabel[MaxPossible];
        for(int i=0; i < hitLabels.length; i++)
            possibleLabels[i] = FrameBase.getLabel("possible "+i + ":", maxWidth);
        int possibleWidth = maxWidth.get();
            
        givenFields = new JTextField[MaxPossible];
        hitFields = new JTextField[MaxPossible];
        possibleFields = new JTextField[MaxPossible];
        models = new Model[MaxPossible];
        for(int i=0; i < models.length; i++)
            models[i] = new Model(0);
        
        for(int i=0; i < stringLabels.length; i++)
        {
            JPanel rowPanel = new JPanel();
            givenFields[i] = new JTextField("", numberOfColumns);
            FrameBase.addTextParamToPanel(stringLabels[i], givenFields[i], stringWidth, -1, "Given Possiblilities", rowPanel);
            givenFields[i].setEditable(true);
            hitFields[i] = new JTextField("", numberOfHitColumns);
            FrameBase.addTextParamToPanel(hitLabels[i], hitFields[i], hitWidth, -1, "Number of hits", rowPanel, this);
            possibleFields[i] = new JTextField("", numberOfColumns);
            FrameBase.addTextParamToPanel(possibleLabels[i], possibleFields[i], possibleWidth, -1, "Possible Secrets", rowPanel);
            hackPanel.add(rowPanel);
        }
        
        mainPanel.add(hackPanel);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(true);
        clearButton = FrameBase.addButtonToPanel("Clear", true, "Clear the entries", buttonPanel, this);
        mainPanel.add(buttonPanel);
        
        JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dataPanel.add(mainPanel);
        
        JScrollPane scrollPane = new JScrollPane(dataPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(600, 450));
        add(scrollPane, BorderLayout.CENTER);
       tstSetup();
       initDone.set(true);
    }

    private void tstSetup()
    {
        for (int i = 0; i < givenFields.length; i++)
            givenFields[i].setText(testGivens[i]);
    }

    /* ************************************************************************
    * ActionListener implementation
    **************************************************************************/

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() != clearButton)
            return;
        for(int i=0; i < models.length; i++)
        {
            models[i].clear();
            for(int j=0; j < givenFields.length; j++)
            {
                givenFields[j].setText("");
                hitFields[j].setText("");
                possibleFields[j].setText("");
            }
        }
    }

    //@formatter:off
    private static final String[] testGivens = new String[]
    {
         "looking",
         "waiting",
         "hooking",
         "banking",
         "calling",
         "abashed",
         "batched",
         "catered",
         "carried",
         "deposed",
    };
    //@formatter:on
    private static final String pass = testGivens[2];

    private void calculate(int index)
    {
        loadModel();
        models[index].hitCnt = Integer.parseInt(hitFields[index].getText());
        for (int i = 0; i < hitFields.length; i++)
            possibleFields[i].setText("");
        String guess = givenFields[index].getText();
//        possibleFields[0].setText(guess);
        // first find all character positions that match
        for(int chrIdx = 0; chrIdx < MaxPossible; chrIdx++)
        {
            for(int modelIdx=0; modelIdx < MaxPossible; modelIdx++)
            {
                if (models[modelIdx].hitCnt != 0)
                {
                }
            }
        }
    }

    private void loadModel()
    {
        if(models[0].passLength == 0)
        {
            int passLength = givenFields[0].getText().length();
            
            for (int i = 0; i < models.length; i++)
            {
                String given = givenFields[i].getText();
                if(given.length() != passLength)
                    FrameBase.displayException(log, this, "Invalid given length", "exp: " + passLength + " rec: " + given.length(), null);
                models[i].passLength = passLength;
                models[i].given = given;
                int[][] match = new int[passLength][MaxPossible];    // given characters, other row matches
                for(int j=0; j < passLength; j++)
                {
                    for(int k=0; k < MaxPossible; k++)
                        match[j][k] = -1;
                }
                models[i].matches = match;
            }
        }
        // find matching column characters that match down the rows
        for(int modelRowIdx=0; modelRowIdx < MaxPossible; modelRowIdx++)
        {
            Model givenModel = models[modelRowIdx]; 
            String given = givenModel.given;
            for(int chrIdx = 0; chrIdx < givenModel.passLength; chrIdx++)
            {
                char chr = given.charAt(chrIdx);
                for(int modelIdx=0; modelIdx < MaxPossible; modelIdx++)
                {
                    String againstGiven = models[modelIdx].given;
                    char againstChr = againstGiven.charAt(chrIdx);
                    if(chr == againstChr)
                        givenModel.matches[chrIdx][modelIdx] = modelIdx;
                }
            }
        }
        
        // get a sorted list of possibles based on most matching columns with other rows
        List<HitCountElement> matches = new ArrayList<HitCountElement>(MaxPossible);
        for(int modelRowIdx=0; modelRowIdx < MaxPossible; modelRowIdx++)
        {
            Model givenModel = models[modelRowIdx];
            HitCountElement match = new HitCountElement(modelRowIdx);
            matches.add(match);
            for(int chrIdx = 0; chrIdx < givenModel.passLength; chrIdx++)
            {
                for(int modelIdx=0; modelIdx < MaxPossible; modelIdx++)
                {
                    int[] rowMatchs =  models[modelRowIdx].matches[chrIdx];
                    if(rowMatchs[modelIdx] != -1)
                        ++match.hitCount;
                }
            }
        }
        Collections.sort(matches);
        setPossibles( matches);
    }
    
    @Override
    public void insertUpdate(DocumentEvent e)
    {
        if (!initDone.get())
            return;
        Document doc = e.getDocument();
        for (int i = 0; i < hitFields.length; i++)
        {
            if (hitFields[i].getDocument() == doc)
            {
                calculate(i);
                break;
            }
        }
    }
    @Override public void removeUpdate(DocumentEvent e){}
    @Override public void changedUpdate(DocumentEvent e){}

    
    
    public void setPossibles(final List<HitCountElement> matches)
    {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                Runnable runner = new Runnable(){@Override public void run()
                {
                    for(int i=0; i < matches.size(); i++)
                    {
                        HitCountElement element = matches.get(i);
                        possibleFields[i].setText(models[element.givenRow].given);
                    }
                }};
                SwingUtilities.invokeLater(runner);
                return null;
            }
        };
        worker.execute();
    }
    
    
    private class Model 
    {
        private int passLength;
        private String given;
        private int hitCnt;
        private int[][] matches;
        
        private Model(int passLength)
        {
            this.passLength = passLength;
        }
        
        private void clear()
        {
            passLength = 0;
            given = "";
            hitCnt = 0;
            for(int i=0; i < MaxPossible; i++, i++)
            {
                for(int j=0; j < passLength; j++)
                {
                    for(int k=0; k < MaxPossible; k++)
                    matches[i][j] = -1;
                }
            }
        }
    }
    
    private class HitCountElement implements Comparable<HitCountElement>
    {
        final int givenRow;
        int hitCount;
        
        private HitCountElement(int givenRow)
        {
            this.givenRow = givenRow;
        }

        @Override
        public int compareTo(HitCountElement o)
        {
           if(hitCount < o.hitCount)
               return -1;
           if(hitCount > o.hitCount)
               return 1;
           return 0;
        }
    }
}
