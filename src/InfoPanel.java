import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class InfoPanel
{
	protected JTextArea log;
	JScrollPane bar;
	public void init()
	{
		log=new JTextArea(8,50);
		bar=new JScrollPane(log);
	}
}
