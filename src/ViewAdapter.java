// Helper class to essentially combine the benefits of KeyAdapter and Mouse*Adapter

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

abstract class ViewAdapter implements MouseListener,
					     MouseMotionListener, 
					     MouseWheelListener,
					     KeyListener
{
    public void keyPressed(KeyEvent k)
    {
    }
    
    public void keyReleased(KeyEvent k)
    {
    }
    
    public void keyTyped(KeyEvent k)
    {
    }

    public void mouseClicked(MouseEvent m)
    {
    }

    public void mouseDragged(MouseEvent m)
    {
    }

    public void mouseEntered(MouseEvent m)
    {
    }

    public void mouseExited(MouseEvent m)
    {
    }

    public void mouseMoved(MouseEvent m)
    {
    }

    public void mousePressed(MouseEvent m)
    {
    }

    public void mouseReleased(MouseEvent m)
    {
    }

    public void mouseWheelMoved(MouseWheelEvent m)
    {
    }

}