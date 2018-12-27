package com.etzwallet.tools.adapter;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.widget.BaseAdapter;

/**
 * @author han
 *
 * @param <T>
 */
public abstract class ListBaseAdapter<T> extends BaseAdapter
{

    protected List<T> mList = null;
    protected Context mContext = null;

    public List<T> getList()
    {
        return mList;
    }

    public boolean isNull()
    {
        return mList == null || mList.size() == 0;
    }

    public ListBaseAdapter(Context mContext)
    {
        this.mContext = mContext;
    }

    /**
     * 添加数据
     *
     * @param t
     */
    public void setmList(List<T> t)
    {
        mList = t;
        notifyDataSetChanged();
    }

    /**
     * 在原集合基础上追加一个集合
     *
     * @param t
     */
    public void addAllList(List<T> t)
    {
        if (t == null)
        {
            return;
        }
        if (mList == null)
        {
            mList = new ArrayList<T>();
        }
        mList.addAll(t);
        notifyDataSetChanged();
    }

    /**
     * 修改某一项的值
     *
     * @param position
     * @param t
     */
    public void setItem(int position, T t)
    {
        if (mList == null || mList.size() == 0)
        {
            return;
        }
        mList.set(position, t);
        notifyDataSetChanged();
    }

    /**
     * 添加数据
     *
     * @param t
     */
    public void addItem(T t)
    {
        if (mList == null)
        {
            mList = new ArrayList<T>();
        }
        mList.add(t);
        notifyDataSetChanged();
    }

    /**
     * 删除某一项
     *
     * @param position
     */
    public void removeItem(int position)
    {
        if (mList == null || mList.size() == 0)
            return;
        mList.remove(position);
        notifyDataSetChanged();
    }

    @Override
    public int getCount()
    {
        return mList == null ? 0 : mList.size();
    }

    @Override
    public T getItem(int position)
    {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }
}
