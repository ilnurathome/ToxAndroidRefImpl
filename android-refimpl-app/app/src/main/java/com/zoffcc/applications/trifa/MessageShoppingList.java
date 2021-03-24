package com.zoffcc.applications.trifa;

import androidx.annotation.Nullable;

import com.github.gfx.android.orma.annotation.Column;
import com.github.gfx.android.orma.annotation.PrimaryKey;
import com.github.gfx.android.orma.annotation.Table;

@Table
public class MessageShoppingList {
    @PrimaryKey(autoincrement = true, auto = true)
    long id; // uniqe message id!!
    @Column(indexed = true, helpers = Column.Helpers.ALL)
    Message message_id;
    @Column(indexed = true, defaultExpr = "false", helpers = Column.Helpers.ALL)
    boolean procceded = false;
    @Column(helpers = Column.Helpers.ALL, defaultExpr = "0")
    @Nullable
    long prcd_timestamp = 0L;
    @Column(helpers = Column.Helpers.ALL, defaultExpr = "0")
    @Nullable
    long prcd_timestamp_ms = 0L;

    public long getId() {
        return id;
    }

    public Message getMessage_id() {
        return message_id;
    }

    public long getMsg_id() {
        return message_id.id;
    }

    public boolean isProcceded() {
        return procceded;
    }

    public void setProcceded(boolean procceded) {
        this.procceded = procceded;
    }
}
