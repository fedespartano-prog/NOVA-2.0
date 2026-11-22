package com.novatwo.assistant.actions;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class NovaAccessibilityService extends AccessibilityService {
    private static volatile NovaAccessibilityService instance;
    public static boolean isEnabled() { return instance != null; }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                    | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                    | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
            setServiceInfo(info);
        }
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public static String execute(String command) {
        NovaAccessibilityService service = instance;
        if (service == null) return "Per controllare le schermate, attiva prima NOVA in Impostazioni > Accessibilità.";
        String value = command == null ? "" : command.trim();
        if (value.equals("indietro")) {
            return service.performGlobalAction(GLOBAL_ACTION_BACK) ? "Sono tornata indietro." : "Azione non disponibile.";
        }
        if (value.equals("home")) {
            return service.performGlobalAction(GLOBAL_ACTION_HOME) ? "Sono tornata alla schermata iniziale." : "Azione non disponibile.";
        }
        if (value.equals("notifiche")) {
            return service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) ? "Ho aperto il pannello notifiche." : "Azione non disponibile.";
        }
        if (value.equals("scorri")) {
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            return root != null && root.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    ? "Ho fatto scorrere la schermata." : "Non trovo una schermata scorrevole.";
        }

        if (value.equals("leggi")) {
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            if (root == null) return "Non riesco a leggere la schermata attiva.";
            StringBuilder visibleText = new StringBuilder();
            collectVisibleText(root, visibleText);
            root.recycle();
            if (visibleText.length() == 0) return "La schermata attiva non espone testo leggibile.";
            String result = visibleText.toString().trim();
            return result.length() > 4000 ? result.substring(0, 4000) : result;
        }
        if (value.startsWith("tocca ")) {
            String label = value.substring(6).trim();
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            AccessibilityNodeInfo target = findText(root, label);
            if (target == null) return "Non trovo un elemento con il testo «" + label + "».";
            AccessibilityNodeInfo clickable = target;
            while (clickable != null && !clickable.isClickable()) {
                AccessibilityNodeInfo parent = clickable.getParent();
                if (clickable != target) clickable.recycle();
                clickable = parent;
            }
            boolean ok = clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (clickable != null && clickable != target) clickable.recycle();
            target.recycle();
            return ok ? "Ho premuto «" + label + "»." : "L'elemento trovato non può essere premuto.";
        }
        if (value.startsWith("scrivi ")) {
            String text = value.substring(7);
            AccessibilityNodeInfo root = service.getRootInActiveWindow();
            AccessibilityNodeInfo target = findEditable(root);
            if (target == null) return "Non trovo un campo di testo modificabile nella schermata.";
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            boolean ok = target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            target.recycle();
            return ok ? "Ho inserito il testo nel campo attivo. Controllalo prima di inviarlo." : "Non riesco a scrivere in quel campo.";
        }
        return "Comando di controllo non riconosciuto.";
    }

    private static void collectVisibleText(AccessibilityNodeInfo node, StringBuilder output) {
        if (node == null || output.length() >= 4000) return;
        CharSequence text = node.getText();
        if (text == null || text.length() == 0) text = node.getContentDescription();
        if (text != null && text.length() > 0) {
            if (output.length() > 0) output.append(" · ");
            output.append(text);
        }
        for (int i = 0; i < node.getChildCount() && output.length() < 4000; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectVisibleText(child, output);
                child.recycle();
            }
        }
    }

    private static AccessibilityNodeInfo findText(AccessibilityNodeInfo node, String text) {
        if (node == null) return null;
        CharSequence label = node.getText();
        if (label != null && label.toString().toLowerCase().contains(text.toLowerCase())) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i=0; i<node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findText(child, text);
            if (child != null) child.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private static AccessibilityNodeInfo findEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable()) return AccessibilityNodeInfo.obtain(node);
        for (int i=0; i<node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo found = findEditable(child);
            if (child != null) child.recycle();
            if (found != null) return found;
        }
        return null;
    }
}