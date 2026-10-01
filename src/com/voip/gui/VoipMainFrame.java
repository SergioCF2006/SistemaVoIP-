package com.voip.gui;

import com.voip.core.CallListener;
import com.voip.core.CallManager;
import com.voip.model.CallHistoryEntry;
import com.voip.model.CallState;
import com.voip.model.SipContact;
import com.voip.registry.DirectIpLocationService;
import com.voip.registry.SipLocationService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana Principal de la Aplicación Sistema VoIP en Java Swing.
 */
public class VoipMainFrame extends JFrame implements CallListener {
    private final CallManager callManager;
    private final SipLocationService locationService;

    // Componentes de Interfaz
    private JLabel lblMyExtension;
    private JLabel lblConnectionStatus;
    private JLabel lblCurrentState;
    private JTextField txtTargetExtension;
    
    private JButton btnCall;
    private JButton btnAccept;
    private JButton btnReject;
    private JButton btnHangUp;
    private JButton btnConfig;

    private DefaultListModel<String> historyListModel;
    private JList<String> lstHistory;
    private JTextArea txtLogConsole;

    // Valores por defecto
    private String remoteExt = "102";
    private String remoteIp = "192.168.1.100";
    private int remoteSipPort = 5060;
    private int remoteRtpPort = 7000;

    public VoipMainFrame() {
        this.locationService = new DirectIpLocationService();
        this.callManager = new CallManager(locationService);
        this.callManager.setListener(this);

        initUi();
        setupDefaultRegistration();
        startVoipService();
    }

    private void initUi() {
        setTitle("Sistema VoIP - Proyecto Sistemas Operativos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(780, 520);
        setMinimumSize(new Dimension(650, 450));
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(10, 10));
        contentPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        contentPane.setBackground(new Color(245, 247, 250));

        // 1. Cabecera (Header)
        contentPane.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. Panel Central (Control de Llamadas e Historial)
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createCallControlPanel(), createHistoryPanel());
        centerSplit.setResizeWeight(0.55);
        centerSplit.setDividerLocation(400);
        contentPane.add(centerSplit, BorderLayout.CENTER);

        // 3. Panel Inferior (Consola de Diagnóstico SIP)
        contentPane.add(createLogPanel(), BorderLayout.SOUTH);

        setContentPane(contentPane);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                callManager.stop();
            }
        });
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 41, 59)); // Dark slate
        headerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel lblTitle = new JLabel("Sistema VoIP");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(Color.WHITE);

        lblMyExtension = new JLabel("Mi Extensión: 101 (" + callManager.getLocalIp() + ")");
        lblMyExtension.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lblMyExtension.setForeground(new Color(148, 163, 184));

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        titlePanel.add(lblTitle);
        titlePanel.add(lblMyExtension);

        lblConnectionStatus = new JLabel("🟢 Conectado [SIP UDP 5060]");
        lblConnectionStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblConnectionStatus.setForeground(new Color(74, 222, 128)); // Light green

        btnConfig = new JButton("⚙ Configurar");
        btnConfig.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnConfig.setFocusPainted(false);
        btnConfig.addActionListener(e -> openConfigDialog());

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);
        rightHeader.add(lblConnectionStatus);
        rightHeader.add(btnConfig);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createCallControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Control de Llamada"));
        panel.setBackground(Color.WHITE);

        // Subpanel Estado
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        statusPanel.setBackground(new Color(241, 245, 249));
        statusPanel.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225)));

        lblCurrentState = new JLabel("Disponible");
        lblCurrentState.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblCurrentState.setForeground(new Color(22, 101, 52)); // Dark green
        statusPanel.add(lblCurrentState);

        // Subpanel Marcación
        JPanel dialPanel = new JPanel(new GridBagLayout());
        dialPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lblTarget = new JLabel("Extensión Destino:");
        lblTarget.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dialPanel.add(lblTarget, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtTargetExtension = new JTextField("102");
        txtTargetExtension.setFont(new Font("Segoe UI", Font.BOLD, 16));
        dialPanel.add(txtTargetExtension, gbc);

        // Subpanel Botones de Acción
        JPanel btnPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        btnPanel.setOpaque(false);

        btnCall = new JButton("📞 Llamar");
        btnCall.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCall.setBackground(new Color(34, 197, 94)); // Green
        btnCall.setForeground(Color.WHITE);
        btnCall.setFocusPainted(false);
        btnCall.addActionListener(e -> {
            String target = txtTargetExtension.getText().trim();
            if (!target.isEmpty()) {
                callManager.makeCall(target);
            } else {
                JOptionPane.showMessageDialog(this, "Ingresa una extensión destino válida.", "Atención", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnAccept = new JButton("✔ Aceptar");
        btnAccept.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnAccept.setBackground(new Color(16, 185, 129)); // Teal Green
        btnAccept.setForeground(Color.WHITE);
        btnAccept.setFocusPainted(false);
        btnAccept.setEnabled(false);
        btnAccept.addActionListener(e -> callManager.acceptCall());

        btnReject = new JButton("✕ Rechazar");
        btnReject.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnReject.setBackground(new Color(239, 68, 68)); // Red
        btnReject.setForeground(Color.WHITE);
        btnReject.setFocusPainted(false);
        btnReject.setEnabled(false);
        btnReject.addActionListener(e -> callManager.rejectCall());

        btnHangUp = new JButton("📵 Colgar");
        btnHangUp.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnHangUp.setBackground(new Color(225, 29, 72)); // Deep Red
        btnHangUp.setForeground(Color.WHITE);
        btnHangUp.setFocusPainted(false);
        btnHangUp.setEnabled(false);
        btnHangUp.addActionListener(e -> callManager.hangUp());

        btnPanel.add(btnCall);
        btnPanel.add(btnAccept);
        btnPanel.add(btnReject);
        btnPanel.add(btnHangUp);

        panel.add(statusPanel, BorderLayout.NORTH);
        panel.add(dialPanel, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Historial de Llamadas"));
        panel.setBackground(Color.WHITE);

        historyListModel = new DefaultListModel<>();
        lstHistory = new JList<>(historyListModel);
        lstHistory.setFont(new Font("Consolas", Font.PLAIN, 12));
        
        JScrollPane scroll = new JScrollPane(lstHistory);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLogPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Registro de Diagnóstico SIP / Red (Logs)"));
        panel.setPreferredSize(new Dimension(750, 130));

        txtLogConsole = new JTextArea();
        txtLogConsole.setEditable(false);
        txtLogConsole.setFont(new Font("Consolas", Font.PLAIN, 11));
        txtLogConsole.setBackground(new Color(15, 23, 42)); // Dark background
        txtLogConsole.setForeground(new Color(148, 163, 184)); // Slate text

        JScrollPane scroll = new JScrollPane(txtLogConsole);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void setupDefaultRegistration() {
        // Registrar extensión remota por defecto en el servicio de ubicación IP directa
        locationService.registerExtension(new SipContact(remoteExt, remoteIp, remoteSipPort, remoteRtpPort));
    }

    private void startVoipService() {
        try {
            callManager.start();
            updateUiState(CallState.DISPONIBLE, "Disponible");
        } catch (Exception e) {
            lblConnectionStatus.setText("🔴 Error en puerto SIP 5060");
            lblConnectionStatus.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "No se pudo abrir el puerto SIP 5060: " + e.getMessage() + "\nPuedes cambiarlo en Configuración.", "Error de Red", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openConfigDialog() {
        ConfigDialog dlg = new ConfigDialog(this, callManager.getLocalIp());
        dlg.setValues(
            callManager.getLocalExtension(), callManager.getLocalIp(), 5060, 7000,
            remoteExt, remoteIp, remoteSipPort, remoteRtpPort
        );
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            this.remoteExt = dlg.getRemoteExt();
            this.remoteIp = dlg.getRemoteIp();
            this.remoteSipPort = dlg.getRemoteSipPort();
            this.remoteRtpPort = dlg.getRemoteRtpPort();

            // Re-configurar endpoint local y registrar nuevo destino
            callManager.configureLocalEndpoint(dlg.getLocalExt(), dlg.getLocalIp(), dlg.getLocalSipPort(), dlg.getLocalRtpPort());
            locationService.registerExtension(new SipContact(remoteExt, remoteIp, remoteSipPort, remoteRtpPort));

            lblMyExtension.setText("Mi Extensión: " + dlg.getLocalExt() + " (" + dlg.getLocalIp() + ")");
            txtTargetExtension.setText(remoteExt);

            try {
                callManager.start();
                lblConnectionStatus.setText("🟢 Conectado [SIP UDP " + dlg.getLocalSipPort() + "]");
                lblConnectionStatus.setForeground(new Color(74, 222, 128));
            } catch (Exception e) {
                lblConnectionStatus.setText("🔴 Error en puerto " + dlg.getLocalSipPort());
                lblConnectionStatus.setForeground(Color.RED);
            }
        }
    }

    // --- Implementación de CallListener ---

    @Override
    public void onStateChanged(CallState newState, String infoMensaje) {
        SwingUtilities.invokeLater(() -> updateUiState(newState, infoMensaje));
    }

    @Override
    public void onIncomingCall(String callerExtension, String callerIp) {
        SwingUtilities.invokeLater(() -> {
            txtTargetExtension.setText(callerExtension);
            // Hacer parpadear o alertar sonido si fuera necesario
        });
    }

    @Override
    public void onHistoryAdded(CallHistoryEntry entry) {
        SwingUtilities.invokeLater(() -> historyListModel.addElement(entry.toString()));
    }

    @Override
    public void onLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            txtLogConsole.append(message + "\n");
            txtLogConsole.setCaretPosition(txtLogConsole.getDocument().getLength());
        });
    }

    private void updateUiState(CallState state, String infoMensaje) {
        lblCurrentState.setText(infoMensaje);

        switch (state) {
            case DISPONIBLE -> {
                lblCurrentState.setForeground(new Color(22, 101, 52));
                btnCall.setEnabled(true);
                btnAccept.setEnabled(false);
                btnReject.setEnabled(false);
                btnHangUp.setEnabled(false);
            }
            case LLAMANDO -> {
                lblCurrentState.setForeground(new Color(194, 65, 12)); // Orange
                btnCall.setEnabled(false);
                btnAccept.setEnabled(false);
                btnReject.setEnabled(false);
                btnHangUp.setEnabled(true);
            }
            case LLAMADA_ENTRANTE -> {
                lblCurrentState.setForeground(new Color(29, 78, 216)); // Blue
                btnCall.setEnabled(false);
                btnAccept.setEnabled(true);
                btnReject.setEnabled(true);
                btnHangUp.setEnabled(false);
            }
            case EN_LLAMADA -> {
                lblCurrentState.setForeground(new Color(15, 118, 110)); // Teal
                btnCall.setEnabled(false);
                btnAccept.setEnabled(false);
                btnReject.setEnabled(false);
                btnHangUp.setEnabled(true);
            }
            case FINALIZADA -> {
                lblCurrentState.setForeground(new Color(185, 28, 28)); // Dark red
                btnCall.setEnabled(false);
                btnAccept.setEnabled(false);
                btnReject.setEnabled(false);
                btnHangUp.setEnabled(false);
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            VoipMainFrame frame = new VoipMainFrame();
            frame.setVisible(true);
        });
    }
}
