package authenticator.Utils;

import java.util.ArrayList;

public class SafeList{
	private ArrayList<Object> data;
	public SafeList(){
		data = new ArrayList<Object>();
	}
	
	public SafeList(ArrayList<Object> arr){
		data = arr;
	}
	
	public synchronized void add(Object object) {
       	data.add(object);
    }
	
	public synchronized void removeAtIndex(int index) {
       	data.remove(index);
    }
	
	public synchronized void remove(Object obj) {
       	data.remove(obj);
    }
	
	public synchronized Object getObjectAtIndex(int index) {
       	return data.get(index);
    }
	
	public synchronized ArrayList<Object> getAll() {
       	return data;
    }
	
	public synchronized void clear(int index) {
       	data.clear();
    }

	public synchronized int size() {
		return data.size();
	}
}
