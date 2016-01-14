/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("javadoc")
public class CalendarDialog extends JDialog
{
    private static final long serialVersionUID = -8382828582671934211L;
    public static final String DateChanged = "dateChanged";
    private final ActionListener callbackListener;

    /**
     * Bring up a date time spinner dialog.
     * @param pframe parent frame to associate this dialog with
     * @param parent component to associate this dialog with
     * @param date text field to obtain starting value and put final value into.
     * @param actionListener  callback listener on changed.  Maybe null for no callback.
     */
    public CalendarDialog(JFrame pframe, JComponent parent, JTextField date, ActionListener actionListener)
    {
        super(pframe, true);
        callbackListener = actionListener;
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(new CalendarSpinnerPanel(this, date));
        setLocationRelativeTo(parent);
        setMinimumSize(new Dimension(245, 160));
        pack();
        setVisible(true);
    }
            
    public class CalendarSpinnerPanel extends JPanel implements ChangeListener, ActionListener 
    {
        private static final long serialVersionUID = 1597315876266772564L;

        private final String[] monthStrings = new String[]
        {
            "January", "February", "March", "April",
            "May", "June", "July", "August", 
            "September", "October", "November", "December"
        };
        
        private final CalendarDialog dialog;
        private final CalendarDialogInfo info;
        
        private final WrappingSpinnerNumberModel yearModel;
        private final WrappingSpinnerListModel monthModel;
        private final WrappingSpinnerNumberModel dayModel;
        private final WrappingSpinnerNumberModel hourModel;
        private final WrappingSpinnerNumberModel minuteModel;
        private final WrappingSpinnerNumberModel secondModel;
        private final WrappingSpinnerNumberModel msModel;
        
        private final JSpinner yearSpinner;
        private final JSpinner monthSpinner;
        private final JSpinner daySpinner;
        private final JSpinner hourSpinner;
        private final JSpinner minuteSpinner;
        private final JSpinner secondSpinner;
        private final JSpinner msSpinner;
        private final JTextField currentValue;

        private final JButton applyButton;
        private final JButton cancelButton;
        
        public CalendarSpinnerPanel(CalendarDialog dialog, JTextField dateIn)
        {
            this(dialog, new CalendarDialogInfo(dateIn));
        }
        
        public CalendarSpinnerPanel(CalendarDialog dialog, CalendarDialogInfo info)
        {
            this.dialog = dialog;
            this.info = info;
            setLayout(new BorderLayout());
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(new TitledBorder("Calendar"));
            
            info.calculate();
            yearModel = new WrappingSpinnerNumberModel(info.year, info.year - 100, info.year + 100, 1);
            monthModel = new WrappingSpinnerListModel(monthStrings);
            monthModel.setValue(monthStrings[info.month]);
            
            dayModel = new WrappingSpinnerNumberModel(info.day, 1, 31, 1);
            if(info.amPm)
                hourModel = new WrappingSpinnerNumberModel(info.hour, 0, 11, 1);
            else
                hourModel = new WrappingSpinnerNumberModel(info.hour, 0, 23, 1);
            minuteModel = new WrappingSpinnerNumberModel(info.minute, 0, 59, 1);
            secondModel = new WrappingSpinnerNumberModel(info.second, 0, 59, 1);
            msModel = new WrappingSpinnerNumberModel(info.milliSecond, 0, 999, 1);
            
            msModel.setLinkedModel(secondModel);
            secondModel.setLinkedModel(minuteModel);
            minuteModel.setLinkedModel(hourModel);
            hourModel.setLinkedModel(dayModel);
            dayModel.setLinkedModel(monthModel);
            monthModel.setLinkedModel(yearModel);
            
            JLabel yearLabel = new JLabel("Year:", SwingConstants.RIGHT);
            JLabel monthLabel = new JLabel("Month:", SwingConstants.RIGHT);
            JLabel dayLabel = new JLabel("Day:", SwingConstants.RIGHT);
            JLabel hourAmLabel = new JLabel("Hour (am/pm):", SwingConstants.RIGHT);
            JLabel hour24Label = new JLabel("Hour (24hr):", SwingConstants.RIGHT);
            JLabel minuteLabel = new JLabel("Minute:", SwingConstants.RIGHT);
            JLabel secondLabel = new JLabel("Second:", SwingConstants.RIGHT);
            JLabel msLabel = new JLabel("ms:", SwingConstants.RIGHT);
            
            JLabel hourLabel = hour24Label;
            if(info.amPm)
                hourLabel = hourAmLabel;
            hourLabel = new JLabel("Hour:", SwingConstants.RIGHT);

            int columnWidth = yearLabel.getPreferredSize().width;
            columnWidth = Math.max(columnWidth, monthLabel.getPreferredSize().width);
            if(info.doDay)
                columnWidth = Math.max(columnWidth, dayLabel.getPreferredSize().width);
            if(info.doDay)
                columnWidth = Math.max(columnWidth, dayLabel.getPreferredSize().width);
            if(info.doHour)
                columnWidth = Math.max(columnWidth, hourLabel.getPreferredSize().width);
            if(info.doMinute)
                columnWidth = Math.max(columnWidth, minuteLabel.getPreferredSize().width);
            if(info.doSecond)
                columnWidth = Math.max(columnWidth, secondLabel.getPreferredSize().width);
            if(info.doMs)
                columnWidth = Math.max(columnWidth, msLabel.getPreferredSize().width);

            yearSpinner = new JSpinner(yearModel);
            FrameBase.addTextParamToPanel(yearLabel, yearSpinner, columnWidth, -1, "Select the desired year.", panel);
            yearSpinner.addChangeListener(this);
            yearSpinner.setEditor(new JSpinner.NumberEditor(yearSpinner, "#"));
            
            monthSpinner = new JSpinner(monthModel);
            FrameBase.addTextParamToPanel(monthLabel, monthSpinner, columnWidth, -1, "Select the desired month.", panel);
            monthSpinner.addChangeListener(this);

            if(info.doDay)
            {
                daySpinner = new JSpinner(dayModel);
                FrameBase.addTextParamToPanel(dayLabel, daySpinner, columnWidth, -1, "Select the desired day.", panel);
                daySpinner.addChangeListener(this);
            }else
                daySpinner = null;
            
            if(info.doHour)
            {
                hourSpinner = new JSpinner(hourModel);
                FrameBase.addTextParamToPanel(hourLabel, hourSpinner, columnWidth, -1, "Select the desired hour.", panel);
                hourSpinner.addChangeListener(this);
            }else
                hourSpinner = null;

            if(info.doMinute)
            {
                minuteSpinner = new JSpinner(minuteModel);
                FrameBase.addTextParamToPanel(minuteLabel, minuteSpinner, columnWidth, -1, "Select the desired minute.", panel);
                minuteSpinner.addChangeListener(this);
            }else
                minuteSpinner = null;

            if(info.doSecond)
            {
                secondSpinner = new JSpinner(secondModel);
                FrameBase.addTextParamToPanel(secondLabel, secondSpinner, columnWidth, -1, "Select the desired second.", panel);
                secondSpinner.addChangeListener(this);
            }else
                secondSpinner = null;

            if(info.doMs)
            {
                msSpinner = new JSpinner(msModel);
                FrameBase.addTextParamToPanel(msLabel, msSpinner, columnWidth, -1, "Select the desired millisecond.", panel);
                msSpinner.addChangeListener(this);
            }else
                msSpinner = null;
            
            
            JPanel datePanel = new JPanel();
            datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.Y_AXIS));
            datePanel.setBorder(new TitledBorder("Date"));
            
            currentValue = new JTextField(info.dateString);
            currentValue.setEditable(false);
            datePanel.add(currentValue);
            panel.add(datePanel);
            
            JPanel buttonPanel = new JPanel();
            buttonPanel.setOpaque(true);
            
            applyButton = FrameBase.addButtonToPanel("Apply", info.dateInBad, "Apply the selected date.", buttonPanel, this);
            cancelButton = FrameBase.addButtonToPanel("Cancel", true, "Cancel any modifications and exit", buttonPanel, this);
            panel.add(buttonPanel);
            
            panel.setSize(800, 600);
            JPanel dataPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            dataPanel.add(panel);
                  
            JScrollPane scrollPane = new JScrollPane(dataPanel);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            add(scrollPane, BorderLayout.CENTER);
            
            Runnable runner = new Runnable(){@SuppressWarnings("synthetic-access")
            @Override public void run(){yearSpinner.requestFocus();}};
            SwingUtilities.invokeLater(runner);
        }

        private String calculate()
        {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, ((Integer)yearModel.getValue()).intValue());
            String month = (String)monthModel.getValue();
            int monthIndex = -1;
            for(int i=0; i < monthStrings.length; i++)
            {
                if(monthStrings[i].equals(month))
                {
                    monthIndex = i;
                    break;
                }
            }
            cal.set(Calendar.MONTH, monthIndex);
            cal.set(Calendar.DAY_OF_MONTH, ((Integer)dayModel.getValue()).intValue());
            int hour = ((Integer)hourModel.getValue()).intValue();
            cal.set(Calendar.HOUR_OF_DAY, hour);
            if(info.amPm)
                cal.set(Calendar.HOUR, hour);
            cal.set(Calendar.MINUTE, ((Integer)minuteModel.getValue()).intValue());
            cal.set(Calendar.SECOND, ((Integer)secondModel.getValue()).intValue());
            cal.set(Calendar.MILLISECOND, ((Integer)msModel.getValue()).intValue());
            return info.getDateString(cal);
        }
        
        @SuppressWarnings("synthetic-access")
        @Override
        public void actionPerformed(ActionEvent e)
        {
            Object src = e.getSource();
            if(src == applyButton)
            {
                info.dateIn.setText(calculate());
                if(callbackListener != null)
                {
                    callbackListener.actionPerformed(new ActionEvent(this, 0, DateChanged));

                }
                dispose();
                return;
            }
            if(src == cancelButton)
            {
                dialog.dispose();
            }
        }
        
        @Override
        public void stateChanged(ChangeEvent e)
        {
            applyButton.setEnabled(true);
            cancelButton.setEnabled(true);
            currentValue.setText(calculate());
        }
        
    }        
    
    public static String DefaultSimpleDateFormat = "MM/dd/yyyy hh:mm:ss:SSS a";  
    
    public static String getDefaultDateString(Date date)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DefaultSimpleDateFormat);
        return sdf.format(date);
    }
    
    static public class CalendarDialogInfo
    {
        public JTextField dateIn;
        public String dateInStr;
        public String simpleDateFormat;
        public Date date;
        public String dateString;
        public boolean dateInBad;
        public Calendar calendar;
        public boolean amPm;
        public boolean doDay;
        public boolean doHour;
        public boolean doMinute;
        public boolean doSecond;
        public boolean doMs;
        
        public int year;
        public int month;
        public int day;
        public int hour;
        public int minute;
        public int second;
        public int milliSecond;
        
        public CalendarDialogInfo(JTextField dateIn)
        {
            this(DefaultSimpleDateFormat, dateIn);
        }
        
        public CalendarDialogInfo(String simpleDateFormat, JTextField dateIn)
        {
            this.simpleDateFormat = simpleDateFormat;
            this.dateIn = dateIn;
            dateInStr = dateIn.getText();
            amPm = false;
            doDay = true;
            doHour = true;
            doMinute = true;
            doSecond = true;
            doMs = true;
            calculate();
        }

        public void calculate()
        {
            SimpleDateFormat sdf = new SimpleDateFormat(simpleDateFormat);
            date = null;
            try{date = sdf.parse(dateIn.getText());}catch(Exception e){date = new Date(System.currentTimeMillis());dateInBad = true;}
            calendar = Calendar.getInstance();
            calendar.setTime(date);
            year = calendar.get(Calendar.YEAR);
            month = calendar.get(Calendar.MONTH);
            day = calendar.get(Calendar.DAY_OF_MONTH);
            hour = calendar.get(Calendar.HOUR_OF_DAY);
            if(amPm)
                hour = calendar.get(Calendar.HOUR);
            minute = calendar.get(Calendar.MINUTE);
            second = calendar.get(Calendar.SECOND);
            milliSecond = calendar.get(Calendar.MILLISECOND);
            boolean changed = false;
            if(doMs)
            {
                doSecond = true;
                doMinute = true;
                doHour = true;
                doDay = true;
                changed = true;
            }else
            if(doSecond)
            {
                doMinute = true;
                doHour = true;
                doDay = true;
                changed = true;
            }else
            if(doMinute)
            {
                doHour = true;
                doDay = true;
                changed = true;
            }else
            if(doHour)
            {
                doHour = true;
                changed = true;
            }
            
            if(!doDay)
            {
                doHour = false;
                doMinute = false;
                doSecond = false;
                doMs = false;
                changed = true;
            }else
            if(!doHour)
            {
                doMinute = false;
                doSecond = false;
                doMs = false;
                changed = true;
            }else
            if(!doMinute)
            {
                doSecond = false;
                doMs = false;
                changed = true;
            }else
            if(!doSecond)
            {
                doMs = false;
                changed = true;
            }
            
            if(!doDay)
                day = 1;
            if(!doHour)
                hour = 0;
            if(!doMinute)
                minute = 0;
            if(!doSecond)
                second = 0;
            if(!doMs)
                milliSecond = 0;
            
            if(changed)
            {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                if(amPm)
                    calendar.set(Calendar.HOUR, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, second);
                calendar.set(Calendar.MILLISECOND, milliSecond);
            }
            date = calendar.getTime();
            dateString = sdf.format(date);   
        }
        
        public String getDateString(Calendar cal)
        {
            SimpleDateFormat sdf = new SimpleDateFormat(simpleDateFormat);
            return sdf.format(cal.getTime());
        }
        
        public Date getValidatedDateIn() throws ParseException
        {
            SimpleDateFormat sdf = new SimpleDateFormat(simpleDateFormat);
            date = sdf.parse(dateInStr);
            calculate();
            return date;
        }

    }
}

