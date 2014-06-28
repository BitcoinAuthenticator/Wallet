package wallettemplate.controls;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class ScrollPaneContentManager extends VBox{
	public ScrollPaneContentManager(){
		super();
		}

	public ScrollPaneContentManager setScrollStyle(String style)
	{
		this.setStyle(style);//"-fx-background-color: white");
		return this;
	}
	
	public ScrollPaneContentManager setSpacingBetweenItems(double spacing)
	{
		this.setSpacing(spacing);
		return this;
	}

	public ScrollPaneContentManager addItem(Node e)
	{
		this.getChildren().add(e);
		return this;
	}

	public void removeNodeAtIndex(int index)
	{
		this.getChildren().remove(index);
	}

	public ScrollPaneContentManager clearAll()
	{
		this.getChildren().clear();
		return this;
	}

	public int getCount()
	{
		return this.getChildren().size();
	}
}