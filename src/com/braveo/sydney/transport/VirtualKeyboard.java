package com.braveo.sydney.transport;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;

public class VirtualKeyboard {
	private Activity parent;
	private View inputTarget;
	
	public VirtualKeyboard(Activity parent){
		this.parent = parent;
		setKeyEvent();
	}
	
	private static final int [] VIRTUAL_KEY_IDS = 
	{
		R.id.BA, R.id.BB, R.id.BC, R.id.BD, R.id.BE, R.id.BF, R.id.BG,
		R.id.BH, R.id.BI, R.id.BJ, R.id.BK, R.id.BL, R.id.BM, R.id.BN,
		R.id.BO, R.id.BP, R.id.BQ, R.id.BR, R.id.BS, R.id.BT,
		R.id.BU, R.id.BV, R.id.BW, R.id.BX, R.id.BY, R.id.BZ, R.id.BBlank,
		R.id.BBack, R.id.BESC, R.id.BClear
	};
	
	public static int Text2KeyCode(char c){
		if(c=='<')
			return KeyEvent.KEYCODE_DEL;
		if(c==' ')
			return KeyEvent.KEYCODE_SPACE;
		if(c>='A' && c<='Z')
		{
			return KeyEvent.KEYCODE_A + (c - 'A');
		}
		assert(false);
		return 0;
	}

	protected void setKeyEvent(){
        for(int k=0; k<VIRTUAL_KEY_IDS.length; k++)
        {
        	Button b = (Button)parent.findViewById(VIRTUAL_KEY_IDS[k]);
        	assert(b!=null);
        	
        	b.setOnClickListener(new View.OnClickListener(){
        		public void onClick(View view){
        			Button bn = (Button)view;
        			if(inputTarget==null)
        				return;
        			
        			if(bn.getId() == R.id.BClear && inputTarget instanceof TextView)
        				((TextView)inputTarget).setText("");
        			else if(bn.getId() == R.id.BESC && inputTarget instanceof AutoCompleteTextView)
        				((AutoCompleteTextView)inputTarget).dismissDropDown();
        			else
        				inputTarget.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, 
        					Text2KeyCode(bn.getText().charAt(0))));
        		}
        	});
        }
        /*
        Button bn = (Button)parent.findViewById(R.id.BNoKey);
        
        bn.setOnClickListener(new View.OnClickListener(){
        	public void onClick(View view){
        		View v = parent.findViewById(R.id.layout_keyboard);
        		if(v.getVisibility() == v.VISIBLE)
        			VirtualKeyboard.this.setVisibility(false);
        		else
        			VirtualKeyboard.this.setVisibility(true);
        	}
        });*/        
	}
	
	public void setInputTarget(View target){
		this.inputTarget = target;
	}
	
	public void setVisibility(boolean visibility){
		View v = parent.findViewById(R.id.layout_keyboard);
		//Button b = (Button)parent.findViewById(R.id.BNoKey);
		
		if(visibility){
			v.setVisibility(View.VISIBLE);
			//b.setText("NoneKeys");
		}else{
			v.setVisibility(View.GONE);
			//b.setText("ShowKeys");
		}
	}
	
}
