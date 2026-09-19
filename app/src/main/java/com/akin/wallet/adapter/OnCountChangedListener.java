package com.akin.wallet.adapter;

/**
 * Filter-result callback shared by the filterable pickers. Lets hosts
 * toggle an empty state after every filter pass. (Was declared identically
 * inside two adapters.)
 */
public interface OnCountChangedListener {
    void onCountChanged(int count);
}
