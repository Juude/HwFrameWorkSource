package com.android.server.print;

import android.print.PrintJobInfo;
import java.util.function.BiConsumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RemotePrintService$KGsYx3sHW6vGymod4UmBTazYSks implements BiConsumer {
    public static final /* synthetic */ -$$Lambda$RemotePrintService$KGsYx3sHW6vGymod4UmBTazYSks INSTANCE = new -$$Lambda$RemotePrintService$KGsYx3sHW6vGymod4UmBTazYSks();

    private /* synthetic */ -$$Lambda$RemotePrintService$KGsYx3sHW6vGymod4UmBTazYSks() {
    }

    public final void accept(Object obj, Object obj2) {
        ((RemotePrintService) obj).handleOnPrintJobQueued((PrintJobInfo) obj2);
    }
}
