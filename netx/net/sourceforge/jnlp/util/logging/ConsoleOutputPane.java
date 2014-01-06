package net.sourceforge.jnlp.util.logging;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.html.HTMLDocument;
import net.sourceforge.jnlp.runtime.JNLPRuntime;
import net.sourceforge.jnlp.util.logging.headers.ObservableMessagesProvider;

public class ConsoleOutputPane extends javax.swing.JPanel implements Observer {

    @Override
    public synchronized void update(Observable o, Object arg) {
        if (!autorefresh.isSelected()) {
            statistics.setText(model.createStatisticHint());
            return;
        }
        boolean passed = model.shouldUpdate();

        if (!passed) {
            statistics.setText(model.createStatisticHint());
            return;
        }
        if (sortBy.getSelectedIndex() == 0) {
            //no sort, we can just update
            updatePane(false);
        } else {
            refreshPane();
        }

    }
    private ConsoleOutputPaneModel model;
    private int lastPostion; //index of search
    private DefaultHighlighter.DefaultHighlightPainter searchHighligh = new DefaultHighlighter.DefaultHighlightPainter(Color.blue);
    private Object lastSearchTag;

    public ConsoleOutputPane(ObservableMessagesProvider dataProvider) {
        model = new ConsoleOutputPaneModel(dataProvider);
        initComponents();
        regExFilter.setText(ConsoleOutputPaneModel.defaultPattern.pattern());
        if (!LogConfig.getLogConfig().isEnableHeaders()) {
            showHeaders.setSelected(false);
        }
        if (JNLPRuntime.isWebstartApplication()) {
            showPlugin.setSelected(false);
            showPreInit.setSelected(false);
            showPostInit.setSelected(false);
            showIncomplete.setSelected(false);
            showComplete.setSelected(false);

            showPlugin.setEnabled(false);
            showPreInit.setEnabled(false);
            showPostInit.setEnabled(false);
            showIncomplete.setEnabled(false);
            showComplete.setEnabled(false);
        }
        regExFilter.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                colorize();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                colorize();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                colorize();
            }

            private void colorize() {
                try {
                    String s = regExFilter.getText();
                    Pattern p = Pattern.compile(s);
                    model.lastValidPattern = p;
                    regExLabel.setForeground(Color.green);
                } catch (Exception ex) {
                    regExLabel.setForeground(Color.red);
                }
            }
        });
        regExFilter.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            if (e.getButton() != MouseEvent.BUTTON3) {
                                insertChars.setVisible(false);
                                return;
                            }
                            insertChars.setLocation(e.getXOnScreen(), e.getYOnScreen());
                            insertChars.setVisible(!insertChars.isVisible());
                        } catch (Exception ex) {
                            OutputController.getLogger().log(ex);
                        }
                    }
                });
            }
        });
        regExFilter.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_CONTEXT_MENU) {
                    return;
                }
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try{
                        insertChars.setLocation(regExFilter.getLocationOnScreen());
                        insertChars.setVisible(!insertChars.isVisible());
                             } catch (Exception ex) {
                            OutputController.getLogger().log(ex);
                        }
                    }
                });
            }
        });

        ButtonGroup matches = new ButtonGroup();
        matches.add(match);
        matches.add(notMatch);
        showHideActionPerformed(null);
        updateModel();
        refreshPane();

    }

    private ActionListener createDefaultAction() {
        return new ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshAction(evt);
            }
        };
    }
    ActionListener defaultActionSingleton = createDefaultAction();

    private ActionListener getDefaultActionSingleton() {
        return defaultActionSingleton;
    }

    private synchronized void refreshPane() {
        if (highLight.isSelected()) {
            jEditorPane1.setContentType("text/html");
        } else {
            jEditorPane1.setContentType("text/plain");
        }
        model.lastUpdateIndex = 0;
        updatePane(true);
    }
    /**
     * when various threads update (and it can be)underlying jeditorpane
     * simultanouskly, then it can lead to unpredictible issues synchroisation
     * is doen in invoe later
     */
    private AtomicBoolean done = new AtomicBoolean(true);

    private synchronized void updatePane(final boolean reset) {
        if (!done.get()) {
            return;
        }
        done.set(false);
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                try {
                    refreshPaneBody(reset);
                } catch (Exception ex) {
                    OutputController.getLogger().log(ex);
                } finally {
                    done.set(true);
                }
            }
        });
    }

    private synchronized void refreshPaneBody(final boolean reset) throws BadLocationException, IOException {
        if (reset) {
            jEditorPane1.setText(model.importList(0));
        } else {
            String s = model.importList();
            if (highLight.isSelected()) {
                HTMLDocument orig = (HTMLDocument) jEditorPane1.getDocument();
                if (revertSort.isSelected()) {
                    orig.insertAfterEnd(orig.getRootElements()[0].getElement(0)/*body*/, s);
                } else {
                    orig.insertBeforeEnd(orig.getRootElements()[0], s);
                }
            } else {
                if (revertSort.isSelected()) {
                    jEditorPane1.setText(s + jEditorPane1.getText());
                } else {
                    jEditorPane1.setText(jEditorPane1.getText() + s);
                }
            }
        }
        jEditorPane1.setCaretPosition(0);
        //jEditorPane1.repaint();
        if (mark.isSelected()) {
            markActionPerformed(null);
        }
        statistics.setText(model.createStatisticHint());
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        showHeaders = new javax.swing.JCheckBox();
        showUser = new javax.swing.JCheckBox();
        sortCopyAll = new javax.swing.JCheckBox();
        showOrigin = new javax.swing.JCheckBox();
        showLevel = new javax.swing.JCheckBox();
        showDate = new javax.swing.JCheckBox();
        showThread1 = new javax.swing.JCheckBox();
        showThread2 = new javax.swing.JCheckBox();
        showMessage = new javax.swing.JCheckBox();
        showOut = new javax.swing.JCheckBox();
        showErr = new javax.swing.JCheckBox();
        showJava = new javax.swing.JCheckBox();
        showPlugin = new javax.swing.JCheckBox();
        showPreInit = new javax.swing.JCheckBox();
        sortByLabel = new javax.swing.JLabel();
        regExLabel = new javax.swing.JCheckBox();
        sortBy = new javax.swing.JComboBox();
        searchLabel = new javax.swing.JLabel();
        autorefresh = new javax.swing.JCheckBox();
        refresh = new javax.swing.JButton();
        apply = new javax.swing.JButton();
        regExFilter = new javax.swing.JTextField();
        //this is crucial, otherwie PalinDocument implementatin is repalcing all \n by space
        ((PlainDocument) regExFilter.getDocument()).getDocumentProperties().remove("filterNewlines");
        copyPlain = new javax.swing.JButton();
        copyRich = new javax.swing.JButton();
        next = new javax.swing.JButton();
        previous = new javax.swing.JButton();
        search = new javax.swing.JTextField();
        caseSensitive = new javax.swing.JCheckBox();
        showIncomplete = new javax.swing.JCheckBox();
        highLight = new javax.swing.JCheckBox();
        wordWrap = new javax.swing.JCheckBox();
        showDebug = new javax.swing.JCheckBox();
        showInfo = new javax.swing.JCheckBox();
        showCode = new javax.swing.JCheckBox();
        statistics = new javax.swing.JLabel();
        showPostInit = new javax.swing.JCheckBox();
        showComplete = new javax.swing.JCheckBox();
        match = new javax.swing.JRadioButton();
        notMatch = new javax.swing.JRadioButton();
        revertSort = new javax.swing.JCheckBox();
        mark = new javax.swing.JCheckBox();
        jScrollPane1 = new javax.swing.JScrollPane();
        jEditorPane1 = new javax.swing.JTextPane();
        showHide = new javax.swing.JButton();

        sortCopyAll.setSelected(true);
        sortCopyAll.setText("sort copy all by date");
        sortCopyAll.setToolTipText("The sort by date is a bit more time consuming, but most natural for posting purposes");
        
        showHeaders.setSelected(true);
        showHeaders.setText("Show headers:");
        showHeaders.addActionListener(getDefaultActionSingleton());

        showUser.setSelected(true);
        showUser.setText("user");
        showUser.addActionListener(getDefaultActionSingleton());

        showOrigin.setSelected(true);
        showOrigin.setText("origin");
        showOrigin.addActionListener(getDefaultActionSingleton());

        showLevel.setSelected(true);
        showLevel.setText("level");
        showLevel.addActionListener(getDefaultActionSingleton());

        showDate.setSelected(true);
        showDate.setText("date");
        showDate.addActionListener(getDefaultActionSingleton());

        showThread1.setSelected(true);
        showThread1.setText("thread 1");
        showThread1.addActionListener(getDefaultActionSingleton());

        showThread2.setSelected(true);
        showThread2.setText("thread 2");
        showThread2.addActionListener(getDefaultActionSingleton());

        showMessage.setSelected(true);
        showMessage.setText("Show messages");
        showMessage.addActionListener(getDefaultActionSingleton());

        showOut.setSelected(true);
        showOut.setText("std. Out");
        showOut.addActionListener(getDefaultActionSingleton());

        showErr.setSelected(true);
        showErr.setText("std. Err");
        showErr.addActionListener(getDefaultActionSingleton());

        showJava.setSelected(true);
        showJava.setText("java");
        showJava.addActionListener(getDefaultActionSingleton());

        showPlugin.setSelected(true);
        showPlugin.setText("plugin");
        showPlugin.addActionListener(getDefaultActionSingleton());

        showPreInit.setSelected(true);
        showPreInit.setText("pre-init");
        showPreInit.setToolTipText("plugin only");
        showPreInit.addActionListener(getDefaultActionSingleton());

        sortByLabel.setText("Sort by:");

        regExLabel.setText("Regular expression filter:");
        regExLabel.addActionListener(getDefaultActionSingleton());

        sortBy.setModel(new javax.swing.DefaultComboBoxModel(new String[]{"As arrived (no sort)", "user", "origin", "level", "date", "code", "thread1", "thread2", "message"}));
        sortBy.addActionListener(getDefaultActionSingleton());

        searchLabel.setText("Search:");

        autorefresh.setSelected(true);
        autorefresh.setText("auto refresh");

        refresh.setText("refresh");
        refresh.addActionListener(getDefaultActionSingleton());

        apply.setText("Apply");
        apply.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                model.usedPattern = model.lastValidPattern;
                refreshAction(evt);
            }
        });

        regExFilter.setText(".*");

        copyPlain.setText("Copy all (plain)");
        copyPlain.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyPlainActionPerformed(evt);
            }
        });

        copyRich.setText("Copy all (rich)");
        copyRich.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyRichActionPerformed(evt);
            }
        });

        next.setText("next>>>");
        next.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextActionPerformed(evt);
            }
        });

        previous.setText("<<<previous");
        previous.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousActionPerformed(evt);
            }
        });

        caseSensitive.setText("case sensitive");

        showIncomplete.setSelected(true);
        showIncomplete.setText("incomplete");
        showIncomplete.setToolTipText("plugin only");
        showIncomplete.addActionListener(getDefaultActionSingleton());

        highLight.setSelected(true);
        highLight.setText("highlight");
        highLight.addActionListener(getDefaultActionSingleton());

        wordWrap.setText("word wrap");
        wordWrap.addActionListener(getDefaultActionSingleton());

        showDebug.setSelected(true);
        showDebug.setText("debug");
        showDebug.addActionListener(getDefaultActionSingleton());

        showInfo.setSelected(true);
        showInfo.setText("info");
        showInfo.addActionListener(getDefaultActionSingleton());

        showCode.setSelected(true);
        showCode.setText("code");
        showCode.addActionListener(getDefaultActionSingleton());

        statistics.setText("x/y");

        showPostInit.setSelected(true);
        showPostInit.setText("post-init");
        showPostInit.setToolTipText("plugin only");
        showPostInit.addActionListener(getDefaultActionSingleton());

        showComplete.setSelected(true);
        showComplete.setText("complete");
        showComplete.setToolTipText("plugin only");
        showComplete.addActionListener(getDefaultActionSingleton());

        match.setSelected(true);
        match.setText("match");
        match.addActionListener(getDefaultActionSingleton());

        notMatch.setText("not");
        notMatch.addActionListener(getDefaultActionSingleton());

        revertSort.setSelected(true);
        revertSort.setText("revert");
        revertSort.addActionListener(getDefaultActionSingleton());

        mark.setText("mark");
        mark.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                markActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(
                javax.swing.GroupLayout.Alignment.LEADING).
                addGroup(
                jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).
                addGroup(
                jPanel2Layout.createSequentialGroup().
                addComponent(showHeaders).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showUser).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).
                addComponent(showOrigin).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showLevel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).
                addComponent(showDate).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showCode).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showThread1).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showThread2)).
                addGroup(jPanel2Layout.createSequentialGroup().addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).addGroup(
                javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                .addComponent(previous).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(mark).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(next).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 15, Short.MAX_VALUE).
                addComponent(wordWrap).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(highLight).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(sortCopyAll).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(copyRich).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(copyPlain)).addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup().
                addComponent(searchLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(search, javax.swing.GroupLayout.DEFAULT_SIZE, 438, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(caseSensitive)).addGroup(
                javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup().
                addComponent(showMessage).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showOut).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showErr).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showJava).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showPlugin).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(showDebug).addGap(6, 6, 6).
                addComponent(showInfo))).addGap(2, 2, 2).
                addComponent(statistics)).addGroup(
                javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup().
                addComponent(showPreInit).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showPostInit).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showIncomplete).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showComplete)).
                addGroup(jPanel2Layout.createSequentialGroup().
                addComponent(autorefresh).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(refresh).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(sortByLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(revertSort).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).
                addComponent(sortBy, 0, 327, Short.MAX_VALUE)).
                addGroup(jPanel2Layout.createSequentialGroup().
                addComponent(regExLabel).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(match).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(notMatch).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(regExFilter, javax.swing.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(apply, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE))).addContainerGap()));
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(
                javax.swing.GroupLayout.Alignment.LEADING).
                addGroup(
                jPanel2Layout.createSequentialGroup().addContainerGap().addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).
                addComponent(showHeaders).
                addComponent(showUser).
                addComponent(showLevel).
                addComponent(showDate).
                addComponent(showOrigin).
                addComponent(showCode).
                addComponent(showThread1).
                addComponent(showThread2)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).
                addComponent(showMessage).
                addComponent(showOut).
                addComponent(showErr).
                addComponent(showJava).
                addComponent(showPlugin).
                addComponent(showDebug).
                addComponent(showInfo).
                addComponent(statistics)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).
                addComponent(showPreInit).
                addComponent(showIncomplete).
                addComponent(showPostInit).
                addComponent(showComplete)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).
                addGroup(
                jPanel2Layout.createSequentialGroup().addGap(32, 32, 32).addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).
                addComponent(regExLabel).addComponent(regExFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).
                addComponent(apply).
                addComponent(match).
                addComponent(notMatch))).
                addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).
                addComponent(autorefresh).
                addComponent(refresh).
                addComponent(sortByLabel).addComponent(sortBy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).
                addComponent(revertSort))).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).
                addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).
                addComponent(searchLabel).addComponent(search, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)).
                addComponent(caseSensitive)).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).
                addComponent(previous).
                addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).
                addComponent(sortCopyAll).
                addComponent(copyPlain).
                addComponent(copyRich).
                addComponent(highLight).
                addComponent(wordWrap)).
                addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).
                addComponent(mark).
                addComponent(next))).addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

        jEditorPane1.setEditable(false);
        jScrollPane1.setViewportView(jEditorPane1);

        showHide.setText("Hide");
        showHide.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showHideActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(this);
        this.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).
                addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().
                addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).
                addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE).
                addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING).
                addComponent(showHide, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 672, Short.MAX_VALUE).
                addComponent(jPanel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)).addContainerGap()))));
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).
                addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup().addContainerGap().
                addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 329, Short.MAX_VALUE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).
                addComponent(showHide).addContainerGap()));

        insertChars = new JPopupMenu();
        JMenuItem tab = new JMenuItem("insert \\t");
        tab.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try{
                        int i = regExFilter.getCaretPosition();
                        StringBuilder s = new StringBuilder(regExFilter.getText());
                        s.insert(i, "\t");
                        regExFilter.setText(s.toString());
                        regExFilter.setCaretPosition(i + 1);
                        insertChars.setVisible(false);
                             } catch (Exception ex) {
                            OutputController.getLogger().log(ex);
                        }
                    }
                });
            }
        });
        JMenuItem newLine = new JMenuItem("insert \\n");
        newLine.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try{
                        int i = regExFilter.getCaretPosition();
                        StringBuilder s = new StringBuilder(regExFilter.getText());
                        s.insert(i, "\n");
                        regExFilter.setText(s.toString());
                        regExFilter.setCaretPosition(i + 1);
                        insertChars.setVisible(false);
                             } catch (Exception ex) {
                            OutputController.getLogger().log(ex);
                        }
                    }
                });
            }
        });

        JMenuItem resetRegex = new JMenuItem("reset default");
        resetRegex.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                java.awt.EventQueue.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        try{
                        regExFilter.setText(ConsoleOutputPaneModel.defaultPattern.pattern());
                        model.lastValidPattern = ConsoleOutputPaneModel.defaultPattern;
                        model.usedPattern = model.lastValidPattern;
                        insertChars.setVisible(false);
                             } catch (Exception ex) {
                            OutputController.getLogger().log(ex);
                        }
                    }
                });
            }
        });

        insertChars.add(newLine);
        insertChars.add(tab);
        insertChars.add(resetRegex);


        validate();
    }

    private void refreshAction(java.awt.event.ActionEvent evt) {
        updateModel();
        refreshPane();
    }

    private void markActionPerformed(java.awt.event.ActionEvent evt) {
        int matches = 0;
        if (!mark.isSelected()) {
            jEditorPane1.getHighlighter().removeAllHighlights();
            return;
        }
        try {
            Document document = jEditorPane1.getDocument();
            String find = search.getText();
            if (find.length() == 0) {
                jEditorPane1.getHighlighter().removeAllHighlights();
                return;
            }
            for (int index = 0; index + find.length() < document.getLength(); index++) {
                String subMatch = document.getText(index, find.length());
                if ((caseSensitive.isSelected() && find.equals(subMatch)) || (!caseSensitive.isSelected() && find.equalsIgnoreCase(subMatch))) {
                    matches++;
                    javax.swing.text.DefaultHighlighter.DefaultHighlightPainter highlightPainter =
                            new javax.swing.text.DefaultHighlighter.DefaultHighlightPainter(Color.orange);
                    jEditorPane1.getHighlighter().addHighlight(index, index + find.length(),
                            highlightPainter);
                }
            }
            mark.setText("mark (" + matches + ")");
        } catch (BadLocationException ex) {
            OutputController.getLogger().log(ex);
        }
    }

    private void previousActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Document document = jEditorPane1.getDocument();
            String find = search.getText();
            if (find.length() == 0) {
                lastPostion = document.getLength() - find.length() - 1;
                return;
            }
            for (int index = lastPostion; index >= 0; index--) {
                String subMatch = document.getText(index, find.length());
                if ((caseSensitive.isSelected() && find.equals(subMatch)) || (!caseSensitive.isSelected() && find.equalsIgnoreCase(subMatch))) {
                    if (lastSearchTag != null) {
                        jEditorPane1.getHighlighter().removeHighlight(lastSearchTag);
                    }
                    lastSearchTag = jEditorPane1.getHighlighter().addHighlight(index, index + find.length(), searchHighligh);
                    jEditorPane1.setCaretPosition(index);

                    lastPostion = index - find.length() - 1;
                    return;
                }
            }
            lastPostion = document.getLength() - find.length() - 1;
        } catch (BadLocationException ex) {
            OutputController.getLogger().log(ex);
        }
    }

    private void nextActionPerformed(java.awt.event.ActionEvent evt) {
        try {
            Document document = jEditorPane1.getDocument();
            String find = search.getText();
            if (find.length() == 0) {
                lastPostion = 0;
                return;
            }
            for (int index = lastPostion; index + find.length() < document.getLength(); index++) {
                String subMatch = document.getText(index, find.length());
                if ((caseSensitive.isSelected() && find.equals(subMatch)) || (!caseSensitive.isSelected() && find.equalsIgnoreCase(subMatch))) {
                    if (lastSearchTag != null) {
                        jEditorPane1.getHighlighter().removeHighlight(lastSearchTag);
                    }
                    lastSearchTag = jEditorPane1.getHighlighter().addHighlight(index, index + find.length(), searchHighligh);
                    jEditorPane1.setCaretPosition(index);

                    lastPostion = index + 1;
                    return;
                }
            }
            lastPostion = 0;
        } catch (BadLocationException ex) {
            OutputController.getLogger().log(ex);
        }
    }

    private void showHideActionPerformed(java.awt.event.ActionEvent evt) {
        if (jPanel2.isVisible()) {
            jPanel2.setVisible(false);
            showHide.setText("Show controls");
        } else {
            jPanel2.setVisible(true);
            showHide.setText("Hide");
        }
    }

    private void copyPlainActionPerformed(java.awt.event.ActionEvent evt) {
        fillClipBoard(false, sortCopyAll.isSelected());
    }

    private void copyRichActionPerformed(java.awt.event.ActionEvent evt) {
        fillClipBoard(true, sortCopyAll.isSelected());
    }
    
    private void fillClipBoard(boolean mark, boolean forceSort){
        StringSelection stringSelection ; 
        if (forceSort){
            stringSelection = new StringSelection(model.importList(mark, 0, 4/*date*/));
        } else {
            stringSelection = new StringSelection(model.importList(mark, 0));
        }
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JFrame dialog = new JFrame();
                dialog.setSize(800, 600);
                dialog.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                ObservableMessagesProvider producer = new ConsoleOutputPaneModel.TestMessagesProvider();
                ConsoleOutputPane jPanel1 = new ConsoleOutputPane(producer);
                producer.getObservable().addObserver(jPanel1);
                dialog.getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
                dialog.pack();
                dialog.setVisible(true);
            }
        });
    }

    private void updateModel() {
        model.highLight = highLight.isSelected();
        model.matchPattern = match.isSelected();
        model.regExLabel = regExLabel.isSelected();
        model.revertSort = revertSort.isSelected();
        model.showCode = showCode.isSelected();
        model.showComplete = showComplete.isSelected();
        model.showDate = showDate.isSelected();
        model.showDebug = showDebug.isSelected();
        model.showErr = showErr.isSelected();
        model.showHeaders = showHeaders.isSelected();
        model.showIncomplete = showIncomplete.isSelected();
        model.showInfo = showInfo.isSelected();
        model.showJava = showJava.isSelected();
        model.showLevel = showLevel.isSelected();
        model.showMessage = showMessage.isSelected();
        model.showOrigin = showOrigin.isSelected();
        model.showOut = showOut.isSelected();
        model.showPlugin = showPlugin.isSelected();
        model.showPostInit = showPostInit.isSelected();
        model.showPreInit = showPreInit.isSelected();
        model.showThread1 = showThread1.isSelected();
        model.showThread2 = showThread2.isSelected();
        model.showUser = showUser.isSelected();
        model.sortBy = sortBy.getSelectedIndex();
        model.wordWrap = wordWrap.isSelected();

    }
    private javax.swing.JButton apply;
    private javax.swing.JCheckBox autorefresh;
    private javax.swing.JCheckBox caseSensitive;
    private javax.swing.JButton copyPlain;
    private javax.swing.JButton copyRich;
    private javax.swing.JCheckBox highLight;
    private javax.swing.JEditorPane jEditorPane1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JCheckBox mark;
    private javax.swing.JRadioButton match;
    private javax.swing.JButton next;
    private javax.swing.JRadioButton notMatch;
    private javax.swing.JButton previous;
    private javax.swing.JButton refresh;
    private javax.swing.JTextField regExFilter;
    private javax.swing.JCheckBox regExLabel;
    private javax.swing.JCheckBox revertSort;
    private javax.swing.JTextField search;
    private javax.swing.JLabel searchLabel;
    private javax.swing.JCheckBox showCode;
    private javax.swing.JCheckBox showComplete;
    private javax.swing.JCheckBox showDate;
    private javax.swing.JCheckBox showDebug;
    private javax.swing.JCheckBox showErr;
    private javax.swing.JCheckBox showHeaders;
    private javax.swing.JButton showHide;
    private javax.swing.JCheckBox showIncomplete;
    private javax.swing.JCheckBox showInfo;
    private javax.swing.JCheckBox showJava;
    private javax.swing.JCheckBox showLevel;
    private javax.swing.JCheckBox showMessage;
    private javax.swing.JCheckBox showOrigin;
    private javax.swing.JCheckBox showOut;
    private javax.swing.JCheckBox showPlugin;
    private javax.swing.JCheckBox showPostInit;
    private javax.swing.JCheckBox showPreInit;
    private javax.swing.JCheckBox showThread1;
    private javax.swing.JCheckBox showThread2;
    private javax.swing.JCheckBox showUser;
    private javax.swing.JCheckBox sortCopyAll;
    private javax.swing.JComboBox sortBy;
    private javax.swing.JLabel sortByLabel;
    private javax.swing.JLabel statistics;
    private javax.swing.JCheckBox wordWrap;
    private javax.swing.JPopupMenu insertChars;
}
