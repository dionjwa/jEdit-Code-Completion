/*
 * SideKickOptionPane.java - SideKick options panel
 * :tabSize=8:indentSize=8:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 2001, 2003 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package completion.options;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.gjt.sp.jedit.AbstractOptionPane;
import org.gjt.sp.jedit.ServiceManager;
import org.gjt.sp.jedit.jEdit;

import completion.service.CompletionProvider;

public class CompletionOptionPane extends AbstractOptionPane
{
    public CompletionOptionPane()
    {
        super("completion");
    }

    @Override
    protected void _init()
    {
        JPanel autoCompletionsPanel = new JPanel();
        autoCompletionsPanel.setBorder(new TitledBorder(jEdit.getProperty(
            "options.completion.auto-completion.label")));
        autoCompletionsPanel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT));
        autoCompletionsPanel.setMinimumSize(new Dimension(1000, 50));


//        JPanel autoCompletionsCheckboxes = new JPanel();
//        autoCompletionsCheckboxes.setLayout(new VerticalLayout(5, VerticalLayout.LEFT));
//        autoCompletionsPanel.add(autoCompletionsCheckboxes, BorderLayout.NORTH);

        autoCompletionsPanel.add(autoCompleteToggle = new JCheckBox(jEdit.getProperty(
        "options.completion.auto-complete.toggle")));
        autoCompleteToggle.setSelected(jEdit.getBooleanProperty("completion.auto-complete.toggle"));
        autoCompleteToggle.addActionListener(new ActionHandler());

        autoCompletePopupGetFocus = new JCheckBox(
            jEdit.getProperty("options.completion.auto-complete-popup-get-focus.toggle"),
            jEdit.getBooleanProperty("completion.auto-complete-popup-get-focus.toggle"));
        autoCompletionsPanel.add(autoCompletePopupGetFocus);

        int autoCompleteDelayValue = jEdit.getIntegerProperty("completion.auto-complete-delay",500);

        autoCompletionsPanel.add(new JLabel(jEdit.getProperty("options.completion.auto-complete-delay")), BorderLayout.CENTER);
        autoCompletionsPanel.add(autoCompleteDelay = new JSlider(0,1500,autoCompleteDelayValue), BorderLayout.SOUTH);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        for(int i = 0; i <= 1500; i += 250)
        {
            labelTable.put(new Integer(i),new JLabel(
                String.valueOf(i / 1000.0)));
        }
        autoCompleteDelay.setLabelTable(labelTable);
        autoCompleteDelay.setPaintLabels(true);
        autoCompleteDelay.setMajorTickSpacing(250);
        autoCompleteDelay.setPaintTicks(true);

        autoCompleteDelay.setEnabled(autoCompleteToggle.isSelected());

        addComponent(autoCompletionsPanel);
        autoCompletionsPanel.setMaximumSize(new Dimension(3000, 1000));
//        autoCompletionsPanel.validate();
//        autoCompletionsPanel.setSize(800, autoCompletionsPanel.getHeight());



        JPanel codeCompletionsPanel = new JPanel();
        codeCompletionsPanel.setBorder(new TitledBorder(jEdit.getProperty(
            "options.completion.code-completion.label")));
        codeCompletionsPanel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT));
        codeCompletionsPanel.setMinimumSize(new Dimension(500, 50));

        // addSeparator("options.completion.code-completion.label");
//        JPanel completionsCheckboxes = new JPanel();
//        completionsCheckboxes.setLayout(new VerticalLayout(5, VerticalLayout.LEFT));

        codeCompletionsPanel.add(allowSelectionByNumbersToggle = new JCheckBox(jEdit.getProperty(
            "options.completion.select-by-numbers.toggle.label")));
        allowSelectionByNumbersToggle.setSelected(jEdit.getBooleanProperty("options.completion.select-by-numbers.toggle"));
        allowSelectionByNumbersToggle.addActionListener(new ActionHandler());

        codeCompletionsPanel.add(completeInstantToggle = new JCheckBox(jEdit.getProperty(
            "options.completion.complete-instant.toggle")));
        completeInstantToggle.setSelected(jEdit.getBooleanProperty("completion.complete-instant.toggle"));
        completeInstantToggle.addActionListener(new ActionHandler());

        codeCompletionsPanel.add(completeDelayToggle = new JCheckBox(jEdit.getProperty(
            "options.completion.complete-delay.toggle")));
        completeDelayToggle.setSelected(jEdit.getBooleanProperty("completion.complete-delay.toggle"));
        completeDelayToggle.addActionListener(new ActionHandler());
//        codeCompletionsPanel.add(completionsCheckboxes, BorderLayout.NORTH);

        int completeDelayValue = jEdit.getIntegerProperty("completion.complete-delay",500);

        codeCompletionsPanel.add(new JLabel(jEdit.getProperty("options.completion.complete-delay")), BorderLayout.CENTER);
        codeCompletionsPanel.add(completeDelay = new JSlider(0,1500,completeDelayValue), BorderLayout.SOUTH);

        addComponent(codeCompletionsPanel);



        labelTable = new Hashtable<Integer, JLabel>();
        for(int i = 0; i <= 1500; i += 250)
        {
            labelTable.put(new Integer(i),new JLabel(
                String.valueOf(i / 1000.0)));
        }
        completeDelay.setLabelTable(labelTable);
        completeDelay.setPaintLabels(true);
        completeDelay.setMajorTickSpacing(250);
        completeDelay.setPaintTicks(true);

        completeDelay.setEnabled(completeDelayToggle.isSelected());

        addComponent(jEdit.getProperty("options.completion.complete-popup.accept-characters"),
            acceptChars = new JTextField(
                jEdit.getProperty("completion.complete-popup.accept-characters")));

        addComponent(jEdit.getProperty("options.completion.complete-popup.insert-characters"),
            insertChars = new JTextField(
                jEdit.getProperty("completion.complete-popup.insert-characters")));

        OrderedListPanel<String> modeOrder = new OrderedListPanel<String>();
        addComponent(modeOrder);
        modeOrder.setList(Arrays.asList(ServiceManager.getServiceNames(CompletionProvider.class)));

    }

    @Override
    protected void _save()
    {
        jEdit.setBooleanProperty("completion.auto-complete.toggle",
            autoCompleteToggle.isSelected());
        jEdit.setIntegerProperty("completion.auto-complete-delay",
            autoCompleteDelay.getValue());

        jEdit.setBooleanProperty("options.completion.select-by-numbers.toggle",
            allowSelectionByNumbersToggle.isSelected());
        jEdit.setBooleanProperty("completion.complete-instant.toggle",
            completeInstantToggle.isSelected());
        jEdit.setBooleanProperty("completion.complete-delay.toggle",
            completeDelayToggle.isSelected());
        jEdit.setIntegerProperty("completion.complete-delay",
            completeDelay.getValue());
        jEdit.setBooleanProperty("completion.auto-complete-popup-get-focus",
            autoCompletePopupGetFocus.isSelected());
        jEdit.setProperty("completion.complete-popup.accept-characters", acceptChars.getText());
        jEdit.setProperty("completion.complete-popup.insert-characters", insertChars.getText());
    }

    //Triggering completion
    private JCheckBox autoCompleteToggle;
    private JSlider autoCompleteDelay;
    private JCheckBox autoCompletePopupGetFocus;

    //After completion popup is shown
    private JCheckBox allowSelectionByNumbersToggle;
    private JCheckBox completeInstantToggle;
    private JCheckBox completeDelayToggle;
    private JSlider completeDelay;
    private JTextField acceptChars;
    private JTextField insertChars;

    class ActionHandler implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent evt)
        {
            Object source = evt.getSource();
            if(source == completeDelayToggle) {
                completeDelay.setEnabled(completeDelayToggle.isSelected());
                acceptChars.setEnabled(completeDelayToggle.isSelected());
            } else if (source == autoCompleteToggle) {
                autoCompleteDelay.setEnabled(autoCompleteToggle.isSelected());
                autoCompletePopupGetFocus.setEnabled(autoCompleteToggle.isSelected());
            }
        }
    }
}
