package starfall.core;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InputManager implements KeyListener, MouseListener, MouseMotionListener {

    private final Set<Integer> pressedKeys = ConcurrentHashMap.newKeySet();
    private final Set<Integer> justPressedKeys = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Character> typedCharacters = new ConcurrentLinkedQueue<>();

    private final AtomicInteger mouseX = new AtomicInteger(0);
    private final AtomicInteger mouseY = new AtomicInteger(0);
    private final AtomicBoolean leftMouseDown = new AtomicBoolean(false);
    private final AtomicBoolean rightMouseDown = new AtomicBoolean(false);
    private final AtomicBoolean leftMouseClicked = new AtomicBoolean(false);
    private volatile boolean allowArrowKeys = true;

    public void setAllowArrowKeys(boolean allowArrowKeys) {
        this.allowArrowKeys = allowArrowKeys;
    }

    public boolean isKeyDown(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    public boolean consumeKeyPress(int keyCode) {
        return justPressedKeys.remove(keyCode);
    }

    public List<Character> drainTypedCharacters() {
        List<Character> chars = new ArrayList<>();
        Character ch;
        while ((ch = typedCharacters.poll()) != null) {
            chars.add(ch);
        }
        return chars;
    }

    public void clearTypedCharacters() {
        typedCharacters.clear();
    }

    public boolean isUpPressed() {
        return isKeyDown(KeyEvent.VK_W) || (allowArrowKeys && isKeyDown(KeyEvent.VK_UP));
    }

    public boolean isDownPressed() {
        return isKeyDown(KeyEvent.VK_S) || (allowArrowKeys && isKeyDown(KeyEvent.VK_DOWN));
    }

    public boolean isLeftPressed() {
        return isKeyDown(KeyEvent.VK_A) || (allowArrowKeys && isKeyDown(KeyEvent.VK_LEFT));
    }

    public boolean isRightPressed() {
        return isKeyDown(KeyEvent.VK_D) || (allowArrowKeys && isKeyDown(KeyEvent.VK_RIGHT));
    }

    public int getMouseX() { return mouseX.get(); }
    public int getMouseY() { return mouseY.get(); }
    public boolean isLeftMouseDown() { return leftMouseDown.get(); }
    public boolean isRightMouseDown() { return rightMouseDown.get(); }

    public boolean consumeLeftClick() {
        return leftMouseClicked.getAndSet(false);
    }

    public Set<Integer> getPressedKeysSnapshot() {
        return Set.copyOf(pressedKeys);
    }

    public void clearAll() {
        pressedKeys.clear();
        justPressedKeys.clear();
        typedCharacters.clear();
        leftMouseDown.set(false);
        rightMouseDown.set(false);
        leftMouseClicked.set(false);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (pressedKeys.add(code)) {
            justPressedKeys.add(code);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
        char ch = e.getKeyChar();
        if (ch != KeyEvent.CHAR_UNDEFINED) {
            typedCharacters.add(ch);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        mouseX.set(e.getX());
        mouseY.set(e.getY());
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftMouseDown.set(true);
            leftMouseClicked.set(true);
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            rightMouseDown.set(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftMouseDown.set(false);
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            rightMouseDown.set(false);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX.set(e.getX());
        mouseY.set(e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX.set(e.getX());
        mouseY.set(e.getY());
    }

    @Override
    public void mouseClicked(MouseEvent e) {}
    @Override
    public void mouseEntered(MouseEvent e) {}
    @Override
    public void mouseExited(MouseEvent e) {}
}