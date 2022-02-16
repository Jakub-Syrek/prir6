import java.util.Comparator;
import java.util.concurrent.*;

public class MotionDetectionSystem implements MotionDetectionSystemInterface {

    private ThreadPoolExecutor executor;
    private ImageConverterInterface imageConverter;
    private ResultConsumerInterface resultConsumer;

    private class ElementToProcess{
        private int frameNumber;
        private int[][] firstImage;
        private int[][] secondImage;

        public ElementToProcess(int frameNumber, int[][] image) {
            if (frameNumber % 2 == 0){
                this.frameNumber = frameNumber;
                this.firstImage = image;
            }else{
                this.frameNumber = frameNumber - 1;
                this.secondImage = image;
            }
        }

        public void setImage(int [][]image){
            if (firstImage == null){
                firstImage = image;
            }else{
                secondImage = image;
            }
        }
    }

    @Override
    public void setThreads(int threads) {

        if (executor == null){
//
            Comparator<Runnable> comparator = (o1, o2) -> ((MyRunnable) o2).getFrameNumber() - ((MyRunnable) o1).getFrameNumber();
            BlockingQueue<Runnable> priorityQueue = new PriorityBlockingQueue<Runnable>(100, comparator);
            executor = new ThreadPoolExecutor(0, threads, 0, TimeUnit.SECONDS, priorityQueue); //TODO: ogarnąć KeepAlive
        }
        executor.setCorePoolSize(0);
        executor.setMaximumPoolSize(threads);
    }

    @Override
    public void setImageConverter(ImageConverterInterface ici) {
        this.imageConverter = ici;
    }

    @Override
    public void setResultListener(ResultConsumerInterface rci) {
        this.resultConsumer = rci;
    }

    @Override
    public void addImage(int frameNumber, int[][] image) {
        imageConverter.convert(
    }

    private class MyRunnable implements Runnable {

        private final int frameNumber;

        public int getFrameNumber() {
            return frameNumber;
        }

        private MyRunnable(int frameNumber) {
            this.frameNumber = frameNumber;
        }

        @Override
        public void run() {

        }
    }
}