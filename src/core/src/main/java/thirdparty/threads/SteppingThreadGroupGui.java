/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package thirdparty.threads;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.gui.LoopControlPanel;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.gui.TestElementMetadata;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.threads.gui.AbstractThreadGroupGui;

@TestElementMetadata(labelResource = "stepping_thread_group")
public class SteppingThreadGroupGui
        extends AbstractThreadGroupGui {

    private class FieldChangesListener implements DocumentListener {

        private final JTextField tf;

        public FieldChangesListener(JTextField field) {
            tf = field;
        }

        private void update() {
            refreshPreview();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            if (tf.hasFocus()) {
                update();
            }
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (tf.hasFocus()) {
                update();
            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            if (tf.hasFocus()) {
                update();
            }
        }
    }

    public static JPanel getComponentWithMargin(Component component, int top, int left, int bottom, int right) {
        JPanel ret = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new java.awt.Insets(top, left, bottom, right);
        ret.add(component, constraints);
        return ret;
    }

    public static final String WIKIPAGE = "SteppingThreadGroup";

    private JTextField initialDelay;
    private JTextField incUserCount;
    private JTextField incUserCountBurst;
    private JTextField incUserPeriod;
    private JTextField flightTime;
    private JTextField decUserCount;
    private JTextField decUserPeriod;
    private JTextField totalThreads;
    private LoopControlPanel loopPanel;
    private JTextField rampUp;

    public SteppingThreadGroupGui() {
        super();
        init();
        initGui();
    }

    protected final void init() {
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(createParamsPanel(), BorderLayout.NORTH);
        add(containerPanel, BorderLayout.CENTER);
        // this magic LoopPanel provides functionality for thread loops
        createControllerPanel();
    }

    @Override
    public void clearGui() {
        super.clearGui();
        initGui();
    }

    // Initialise the gui field values
    private void initGui() {
        totalThreads.setText("100");
        initialDelay.setText("0");
        incUserCount.setText("10");
        incUserCountBurst.setText("0");
        incUserPeriod.setText("30");
        flightTime.setText("60");
        decUserCount.setText("5");
        decUserPeriod.setText("1");
        rampUp.setText("5");
    }

    private JPanel createParamsPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 5, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Threads Scheduling Parameters"));

        panel.add(new JLabel("This group will start", JLabel.RIGHT));
        totalThreads = new JTextField(5);
        panel.add(totalThreads);
        panel.add(new JLabel("threads:", JLabel.LEFT));
        panel.add(new JLabel());
        panel.add(new JLabel());

        panel.add(new JLabel("First, wait for", JLabel.RIGHT));
        initialDelay = new JTextField(5);
        panel.add(initialDelay);
        panel.add(new JLabel("seconds;", JLabel.LEFT));
        panel.add(new JLabel());
        panel.add(new JLabel());

        panel.add(new JLabel("Then start", JLabel.RIGHT));
        incUserCountBurst = new JTextField(5);
        panel.add(incUserCountBurst);
        panel.add(new JLabel("threads; ", JLabel.LEFT));
        panel.add(new JLabel(""));
        panel.add(new JLabel());

        panel.add(new JLabel("Next, add", JLabel.RIGHT));
        incUserCount = new JTextField(5);
        panel.add(incUserCount);
        panel.add(new JLabel("threads every", JLabel.CENTER));
        incUserPeriod = new JTextField(5);
        panel.add(incUserPeriod);
        panel.add(new JLabel("seconds, ", JLabel.LEFT));

        panel.add(new JLabel());
        panel.add(new JLabel());
        panel.add(new JLabel("using ramp-up", JLabel.RIGHT));
        rampUp = new JTextField(5);
        panel.add(rampUp);
        panel.add(new JLabel("seconds.", JLabel.LEFT));

        panel.add(new JLabel("Then hold load for", JLabel.RIGHT));
        flightTime = new JTextField(5);
        panel.add(flightTime);
        panel.add(new JLabel("seconds.", JLabel.LEFT));
        panel.add(new JLabel());
        panel.add(new JLabel());

        panel.add(new JLabel("Finally, stop", JLabel.RIGHT));
        decUserCount = new JTextField(5);
        panel.add(decUserCount);
        panel.add(new JLabel("threads every", JLabel.CENTER));
        decUserPeriod = new JTextField(5);
        panel.add(decUserPeriod);
        panel.add(new JLabel("seconds.", JLabel.LEFT));

        registerJTextfieldForGraphRefresh(totalThreads);
        registerJTextfieldForGraphRefresh(initialDelay);
        registerJTextfieldForGraphRefresh(incUserCount);
        registerJTextfieldForGraphRefresh(incUserCountBurst);
        registerJTextfieldForGraphRefresh(incUserPeriod);
        registerJTextfieldForGraphRefresh(flightTime);
        registerJTextfieldForGraphRefresh(decUserCount);
        registerJTextfieldForGraphRefresh(decUserPeriod);
        registerJTextfieldForGraphRefresh(rampUp);

        return panel;
    }

    @Override
    public String getLabelResource() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getStaticLabel() {
        return "Stepping Thread Group";
    }

    @Override
    public TestElement createTestElement() {
        SteppingThreadGroup tg = new SteppingThreadGroup();
        modifyTestElement(tg);
        tg.setComment("Stepping Thread Group (sourced from JMeter Plugins Marketplace.)");
        return tg;
    }

    private void refreshPreview() {
        SteppingThreadGroup tgForPreview = new SteppingThreadGroup();
        tgForPreview.setNumThreads(new CompoundVariable(totalThreads.getText()).execute());
        tgForPreview.setThreadGroupDelay(new CompoundVariable(initialDelay.getText()).execute());
        tgForPreview.setInUserCount(new CompoundVariable(incUserCount.getText()).execute());
        tgForPreview.setInUserCountBurst(new CompoundVariable(incUserCountBurst.getText()).execute());
        tgForPreview.setInUserPeriod(new CompoundVariable(incUserPeriod.getText()).execute());
        tgForPreview.setOutUserCount(new CompoundVariable(decUserCount.getText()).execute());
        tgForPreview.setOutUserPeriod(new CompoundVariable(decUserPeriod.getText()).execute());
        tgForPreview.setFlightTime(new CompoundVariable(flightTime.getText()).execute());
        tgForPreview.setRampUp(new CompoundVariable(rampUp.getText()).execute());

        if (tgForPreview.getInUserCountAsInt() == 0) {
            tgForPreview.setInUserCount(new CompoundVariable(totalThreads.getText()).execute());
        }
        if (tgForPreview.getOutUserCountAsInt() == 0) {
            tgForPreview.setOutUserCount(new CompoundVariable(totalThreads.getText()).execute());
        }

        updateChart(tgForPreview);
    }

    @Override
    public void modifyTestElement(TestElement te) {
        super.configureTestElement(te);

        if (te instanceof SteppingThreadGroup) {
            SteppingThreadGroup tg = (SteppingThreadGroup) te;
            tg.setProperty(SteppingThreadGroup.NUM_THREADS, totalThreads.getText());
            tg.setThreadGroupDelay(initialDelay.getText());
            tg.setInUserCount(incUserCount.getText());
            tg.setInUserCountBurst(incUserCountBurst.getText());
            tg.setInUserPeriod(incUserPeriod.getText());
            tg.setOutUserCount(decUserCount.getText());
            tg.setOutUserPeriod(decUserPeriod.getText());
            tg.setFlightTime(flightTime.getText());
            tg.setRampUp(rampUp.getText());
            tg.setSamplerController((LoopController) loopPanel.createTestElement());

            refreshPreview();
        }
    }

    @Override
    public void configure(TestElement te) {
        super.configure(te);
        SteppingThreadGroup tg = (SteppingThreadGroup) te;
        totalThreads.setText(tg.getNumThreadsAsString());
        initialDelay.setText(tg.getThreadGroupDelay());
        incUserCount.setText(tg.getInUserCount());
        incUserCountBurst.setText(tg.getInUserCountBurst());
        incUserPeriod.setText(tg.getInUserPeriod());
        decUserCount.setText(tg.getOutUserCount());
        decUserPeriod.setText(tg.getOutUserPeriod());
        flightTime.setText(tg.getFlightTime());
        rampUp.setText(tg.getRampUp());

        TestElement controller = (TestElement) tg.getProperty(AbstractThreadGroup.MAIN_CONTROLLER).getObjectValue();
        if (controller != null) {
            loopPanel.configure(controller);
        }
    }

    private void updateChart(SteppingThreadGroup tg) {

    }

    private JPanel createControllerPanel() {
        loopPanel = new LoopControlPanel(false);
        LoopController looper = (LoopController) loopPanel.createTestElement();
        looper.setLoops(-1);
        looper.setContinueForever(true);
        loopPanel.configure(looper);
        return loopPanel;
    }

    private void registerJTextfieldForGraphRefresh(final JTextField tf) {
        tf.addActionListener(loopPanel);
        tf.getDocument().addDocumentListener(new FieldChangesListener(tf));
    }
}
