package com.akin.wallet.adapter;

/**
 * Filter-result callback shared by the filterable pickers. Lets hosts
 * toggle an empty state after every filter pass.
 */
public interface OnCountChangedListener {
    void onCountChanged(int count);
}
