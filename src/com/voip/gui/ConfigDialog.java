package com.voip.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo de configuración de Extensión, IP Local, IP Remota y Puertos UDP.
 */
public class ConfigDialog extends JDialog {
    private final JTextField txtLocalExt = new JTextField("101", 10);
    private final JTextField txtLocalIp = new JTextField("", 15);
    private final JTextField txtLocalSipPort = new JTextField("5060", 6);
    private final JTextField txtLocalRtpPort = new JTextField("7000", 6);

    private final JTextField txtRemoteExt = new JTextField("102", 10);
    private final JTextField txtRemoteIp = new JTextField("192.168.1.100", 15);
    private final JTextField txtRemoteSipPort = new JTextField("5060", 6);
    private final JTextField txtRemoteRtpPort = new JTextField("7000", 6);

    private boolean confirmed = false;

    public ConfigDialog(Frame owner, String localIp) {
        super(owner, "Configuración del Endpoint VoIP", true);
        txtLocalIp.setText(localIp);
        initUi();
    }

    private void initUi() {
        setLayout(new BorderLayout(10, 10));
        setSize(420, 360);
        setLocationRelativeTo(getOwner());

        JPanel mainPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel Local
        JPanel localPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        localPanel.setBorder(BorderFactory.createTitledBorder("Configuración de esta PC (Local)"));
        localPanel.add(new JLabel("Mi Extensión:"));
        localPanel.add(txtLocalExt);
        localPanel.add(new JLabel("Mi IP Local:"));
        localPanel.add(txtLocalIp);
        localPanel.add(new JLabel("Puerto SIP (UDP):"));
        localPanel.add(txtLocalSipPort);
        localPanel.add(new JLabel("Puerto RTP Audio:"));
        localPanel.add(txtLocalRtpPort);

        // Panel Remoto (Fase 1 IP Directa)
        JPanel remotePanel = new JPanel(new GridLayout(4, 2, 5, 5));
        remotePanel.setBorder(BorderFactory.createTitledBorder("Configuración de Destino (IP Directa / Extensión)"));
        remotePanel.add(new JLabel("Extensión Destino:"));
        remotePanel.add(txtRemoteExt);
        remotePanel.add(new JLabel("IP Destino:"));
        remotePanel.add(txtRemoteIp);
        remotePanel.add(new JLabel("Puerto SIP Destino:"));
        remotePanel.add(txtRemoteSipPort);
        remotePanel.add(new JLabel("Puerto RTP Destino:"));
        remotePanel.add(txtRemoteRtpPort);

        mainPanel.add(localPanel);
        mainPanel.add(remotePanel);

        // Botones
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnSave = new JButton("Guardar y Aplicar");
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        btnSave.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());

        btnPanel.add(btnCancel);
        btnPanel.add(btnSave);

        add(mainPanel, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);
    }

    public boolean isConfirmed() { return confirmed; }
    public String getLocalExt() { return txtLocalExt.getText().trim(); }
    public String getLocalIp() { return txtLocalIp.getText().trim(); }
    public int getLocalSipPort() { return parsePort(txtLocalSipPort.getText(), 5060); }
    public int getLocalRtpPort() { return parsePort(txtLocalRtpPort.getText(), 7000); }

    public String getRemoteExt() { return txtRemoteExt.getText().trim(); }
    public String getRemoteIp() { return txtRemoteIp.getText().trim(); }
    public int getRemoteSipPort() { return parsePort(txtRemoteSipPort.getText(), 5060); }
    public int getRemoteRtpPort() { return parsePort(txtRemoteRtpPort.getText(), 7000); }

    public void setValues(String lExt, String lIp, int lSip, int lRtp, String rExt, String rIp, int rSip, int rRtp) {
        txtLocalExt.setText(lExt);
        txtLocalIp.setText(lIp);
        txtLocalSipPort.setText(String.valueOf(lSip));
        txtLocalRtpPort.setText(String.valueOf(lRtp));
        txtRemoteExt.setText(rExt);
        txtRemoteIp.setText(rIp);
        txtRemoteSipPort.setText(String.valueOf(rSip));
        txtRemoteRtpPort.setText(String.valueOf(rRtp));
    }

    private int parsePort(String text, int defaultPort) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return defaultPort;
        }
    }
}
