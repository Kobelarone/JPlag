package de.jplag.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides static access to the creation of progress bars.
 */
public class ProgressBarLogger {
    private static ProgressBarProvider progressBarProvider = new DummyProvider();

    private ProgressBarLogger() {
        //Hides default constructor
    }

    /**
     * Creates a new {@link ProgressBar}
     * @param type The type of the progress bar
     * @param totalSteps The total number of steps
     * @return The newly created progress bar
     */
    public static ProgressBar createProgressBar(ProgressBarType type, int totalSteps) {
        return progressBarProvider.initProgressBar(type, totalSteps);
    }

    /**
     * Sets the {@link ProgressBarProvider}. Should be used by the ui before calling JPlag, if progress bars should be
     * shown.
     * @param progressBarProvider The provider
     */
    public static void setProgressBarProvider(ProgressBarProvider progressBarProvider) {
        ProgressBarLogger.progressBarProvider = progressBarProvider;
    }

    private static class DummyProvider implements ProgressBarProvider {
        @Override
        public ProgressBar initProgressBar(ProgressBarType type, int totalSteps) {
            return new DummyBar(type, totalSteps);
        }
    }

    private static class DummyBar implements ProgressBar {
        private static final Logger logger = LoggerFactory.getLogger(DummyBar.class);
        private int currentStep;

        public DummyBar(ProgressBarType type, int totalSteps) {
            this.currentStep = 0;
            logger.info("{} ({})", type.getDefaultText(), totalSteps);
        }

        @Override
        public void step() {
            logger.info("Now at step {}", this.currentStep++);
        }

        @Override
        public void step(int number) {
            for (int i = 0; i < number; i++) {
                step();
            }
        }

        @Override
        public void dispose() {
            logger.info("Progress bar done.");
        }
    }
}
