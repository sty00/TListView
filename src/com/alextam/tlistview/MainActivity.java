package com.alextam.tlistview;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.alextam.tlistview.TListView.PULL_MODE;
import com.alextam.tlistview.TListView.onTListViewListener;

public class MainActivity extends Activity {
	private ArrayAdapter<String> adapter;
	private List<String> datas = new ArrayList<String>();
	private TListView lv_main;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		lv_main = (TListView)findViewById(R.id.lv_main);
		lv_main.setTListViewMode(PULL_MODE.BOTH_PULL);
		init();
		
	}
	
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg)
		{
			if(msg.what == 0x101)
			{
				datas.add("刷新数据 " + datas.size());
				adapter.notifyDataSetChanged();
				Toast.makeText(MainActivity.this, "刷新成功~", Toast.LENGTH_SHORT).show();
			}
			else if(msg.what == 0x102)
			{
				for(int i=0; i<3; i++)
				{
					datas.add("增加数据 " + datas.size());
				}
				adapter.notifyDataSetChanged();
				Toast.makeText(MainActivity.this, "加载更多~", Toast.LENGTH_SHORT).show();	
			}
			super.handleMessage(msg);
		}
	};
	
	private void init()
	{
		for(int i=0;i<10; i++)
		{
			datas.add("数据 " + i);
		}
		
		lv_main.setOnTListViewListener(new onTListViewListener() {
			@Override
			public void onRefresh() {
				try {
					Thread.sleep(3000);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				mHandler.sendEmptyMessage(0x101);
				
			}
			
			@Override
			public void onLoadMore() {
				try {
					Thread.sleep(3000);
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				mHandler.sendEmptyMessage(0x102);
			}
		});
		
		adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_dropdown_item_1line, datas);
		lv_main.setAdapter(adapter);
	}


}
