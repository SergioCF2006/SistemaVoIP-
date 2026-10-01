package com.voip.sip;

/**
 * Listener que recibe eventos parseados de la capa de señalización SIP.
 */
public interface SipSignalingListener {
    void onInviteReceived(SipParser.ParsedMessage msg);
    void onRingingReceived(SipParser.ParsedMessage msg);
    void onOkReceived(SipParser.ParsedMessage msg);
    void onAckReceived(SipParser.ParsedMessage msg);
    void onByeReceived(SipParser.ParsedMessage msg);
    void onBusyReceived(SipParser.ParsedMessage msg);
}
