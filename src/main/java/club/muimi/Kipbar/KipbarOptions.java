package club.muimi.Kipbar;

import java.io.PrintStream;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class KipbarOptions {
    @Builder.Default
    private final char progressChar = '#';

    @Builder.Default
    private final boolean printDoneMessage = true;

    @Builder.Default
    private final boolean showPercentage = true;

    @Builder.Default
    private final boolean showElapsedTime = true;

    @Builder.Default
    private final PrintStream output = System.out;

    private final String taskName;
    private final String failureMessage;
}
