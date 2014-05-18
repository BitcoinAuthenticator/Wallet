package wallettemplate.controls;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class ScrollPaneContentManager extends VBox{
	public ScrollPaneContentManager(){
		super();
		
		}
	
	public ScrollPaneContentManager setSpacingBetweenItems(double spacing)
	{
		this.setSpacing(10);
		return this;
	}
	
	public ScrollPaneContentManager addItem(Node e)
	{
		this.getChildren().add(e);
		return this;
	}
	
	public ScrollPaneContentManager removeNodeAtIndex(int index)
	{
		ObservableList<Node> lst = this.getChildren();
		lst.remove(index);
		this.getChildren().clear();
		this.getChildren().setAll(lst);
		return this;
	}
	
	public ScrollPaneContentManager clearAll()
	{
		this.getChildren().clear();
		return this;
	}
}
