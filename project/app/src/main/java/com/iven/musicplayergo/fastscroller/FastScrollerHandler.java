package com.iven.musicplayergo.fastscroller;

import android.os.Handler;
import android.os.Message;

class FastScrollerHandler extends Handler {

    static final int INDEX_THUMB = 500;
    static final int INDEX_BAR = 3000;

    static class IndexThumbHandler extends Handler {
        private FastScrollerRecyclerView mRecyclerView;

        IndexThumbHandler(FastScrollerRecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == INDEX_THUMB) {
                mRecyclerView.invalidate();
            }
        }
    }

    static class IndexBarHandler extends Handler {
        private FastScrollerView mFastScroller;

        IndexBarHandler(FastScrollerView fastScrollerView) {
            mFastScroller = fastScrollerView;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == INDEX_BAR) {
                mFastScroller.setHidden();
            }
        }
    }
}