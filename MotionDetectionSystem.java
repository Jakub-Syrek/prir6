import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;



public class MotionDetectionSystem implements MotionDetectionSystemInterface {

    private ThreadPoolExecutor executor;
    private ThreadPoolExecutor executor2;

    private ImageConverterInterface imageConverter;
    private ResultConsumerInterface resultConsumer;
    //private ConcurrentSkipListSet<ElementToProcess> elementsList = new ConcurrentSkipListSet<>();
    private ArrayList<ElementToProcess> elementsList = new ArrayList<>(100);
    private ArrayList<Point2D.Double> returnList = new ArrayList<>(100);

    private  ConcurrentHashMap< Integer, int[][] > singleImagesMap = new ConcurrentHashMap<>();

    public AtomicInteger returnedCounter = new AtomicInteger(-1);

    public static int GetChunk(int num){
        double s = num / 2.0;
        String dbl = Double.toString(s);
        String res = dbl.substring(0,dbl.indexOf("."));
        return Integer.valueOf(res);
    }
    private int currentThreadsCount = 10;


    private class ResultWrapper implements Runnable{
        public Point2D.Double point2D = null;
        public int frameNumber = -1;
        private ResultConsumerInterface resultConsumerInterface = null;

        public ResultWrapper(Point2D.Double point2D, int frameNumber, ResultConsumerInterface resultConsumerInterface)  {
            this.point2D = point2D;
            this.frameNumber = frameNumber;
            this.resultConsumerInterface = resultConsumerInterface;
        }

        @Override
        public void run() {
//            while (frameNumber != returnedCounter.intValue() + 1){
//                try {
//                    Thread.sleep(10);
//                    //this.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
            //this.notifyAll();
            resultConsumerInterface.accept(frameNumber, point2D);
            returnedCounter.incrementAndGet();
        }

//        @Override
//        protected final <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
//            final RunnableFuture<T> task = super.newTaskFor(runnable, value);
//            futures.add(task);
//            return task;
//        }
    }

    private class RejectedExecutionHandlerImpl implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            try {
                executor2.execute(r);
            }
            catch(Exception e)
            {

            }
            //System.out.println(r.toString() + " is rejected");
        }

    }

    private class ElementToProcess implements Comparable<ElementToProcess>{
        public Integer elementNumber = -1;
        private Integer firstFrameNumber = -1;
        private Integer secondFrameNumber = -1;

        public ElementToProcess(Integer elementNumber, int[][] firstImage, int[][] secondImage) {
            this.elementNumber = elementNumber;
            this.firstImage = firstImage;
            this.secondImage = secondImage;
        }

        private int[][] firstImage = null;
        private int[][] secondImage = null;

        public ElementToProcess(int frameNumber, int[][] image) {

            this.elementNumber = GetChunk(frameNumber);

            if (frameNumber % 2 == 0){
                this.firstFrameNumber = frameNumber;
                this.firstImage = image;
            }else{
                this.secondFrameNumber = frameNumber;
                this.secondImage = image;
            }
        }

        public void setImage(int [][]image){
            if (firstImage == null){
                firstImage = image;
                this.firstFrameNumber = this.secondFrameNumber - 1;
            }else{
                secondImage = image;
                this.secondFrameNumber = this.firstFrameNumber + 1;
            }
        }
        @Override
        public int compareTo(ElementToProcess o) {
            final long diff = o.elementNumber - elementNumber;
            return 0 == diff ? 0 : 0 > diff ? -1 : 1;
            //return this.elementNumber.compareTo(o.elementNumber);
        }
    }

    class PriorityFuture<T> implements RunnableFuture<T> {

        private RunnableFuture<T> src;
        private int priority;

        public PriorityFuture(RunnableFuture<T> other, int priority) {
            this.src = other;
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return src.cancel(mayInterruptIfRunning);
        }

        public boolean isCancelled() {
            return src.isCancelled();
        }

        public boolean isDone() {
            return src.isDone();
        }

        public T get() throws InterruptedException, ExecutionException {
            return src.get();
        }

        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return src.get();
        }

        public void run() {
            src.run();
        }
    }

    class PriorityFutureComparator implements Comparator<Runnable> {
        public int compare(Runnable o1, Runnable o2) {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return -1;
            else if (o2 == null)
                return 1;
            else {
                int p1 = ((PriorityFuture<?>) o1).getPriority();
                int p2 = ((PriorityFuture<?>) o2).getPriority();

                return p1 > p2 ? 1 : (p1 == p2 ? 0 : -1);
            }
        }
    }

    @Override
    public void setThreads(int threads) {

        int previousThreadsCount = currentThreadsCount;
        currentThreadsCount = threads;

        if (executor == null){
//            Comparator<Runnable> comparator = new Comparator<Runnable>() {
//                @Override
//                public int compare(Runnable o1, Runnable o2) {
//
//                    return ((ResultReturnerRunnable) o2).frameNumber.compareTo(((ResultReturnerRunnable) o1).frameNumber);
//                            //compareTo(((ResultReturnerRunnable) o1));
//                }
//
//                @Override
//                public boolean equals(Object obj) {
//                    return (this).equals(obj);
//                }
//            };
            //comparing(ResultReturnerRunnable::getFrameNumber);
            //Comparator<Runnable> comparator = (o1, o2) -> ((ResultReturnerRunnable) o2).compareTo(((ResultReturnerRunnable) o1));
            //RunnableFuture
            //BlockingQueue<Runnable> priorityQueue = new PriorityBlockingQueue<Runnable>(200, comparator);
            //BlockingQueue<Runnable> priorityQueue = (BlockingQueue<Runnable>) new PriorityQueue<Runnable>(200, comparator);

            BlockingQueue<Runnable> priorityQueue = new ArrayBlockingQueue<Runnable>(100);
            executor = new ThreadPoolExecutor(0, currentThreadsCount, 0L, TimeUnit.MILLISECONDS,
                    new PriorityBlockingQueue<Runnable>(100, new PriorityFutureComparator())) {
                protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                    RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                    return new PriorityFuture<T>(newTaskFor, ((ResultReturnerCallable) callable).getPriority());
                }
            };
            //new ThreadPoolExecutor(0, 10, 10000, TimeUnit.MILLISECONDS, priorityQueue, new RejectedExecutionHandlerImpl());
            //(10000, 10000, 10000, TimeUnit.MILLISECONDS, priorityQueue);
            //(ThreadPoolExecutor)Executors.newCachedThreadPool();

            executor.setMaximumPoolSize(100);
            executor.setCorePoolSize(100);
            executor.prestartAllCoreThreads();

            executor2 = (ThreadPoolExecutor)Executors.newCachedThreadPool();
            executor2.setCorePoolSize(100);

        }

        if (threads <= executor.getMaximumPoolSize()) {
            executor.setCorePoolSize(threads);
            executor.setMaximumPoolSize(threads);
        } else {
            executor.setMaximumPoolSize(threads);
            executor.setCorePoolSize(threads);
        }
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
    public void addImage(int frameNumber, int[][] newImage) {

        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int myElementNumber = GetChunk(frameNumber);

        var check = singleImagesMap.containsKey(myElementNumber);
        if(check){
            int[][] firstImage = null;
            Point2D.Double d = null;
            firstImage = singleImagesMap.get(myElementNumber);

            ElementToProcess element = new ElementToProcess(myElementNumber, firstImage, newImage);

            var res  = this.executor.submit(new ResultReturnerCallable(myElementNumber, firstImage, newImage, imageConverter, resultConsumer));
                    //new ResultReturnerCallable(element, this.imageConverter, this.resultConsumer));
            try {
                d = (Point2D.Double) res.get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }else{
            singleImagesMap.put(myElementNumber, newImage);
        }




//
//        ElementToProcess myElement = null;
//        synchronized (elementsList) {
//            myElement = this.elementsList.stream().filter(elementToProcess -> elementToProcess.elementNumber == myElementNumber).findFirst().orElse(null);
//        }
//        if(myElement != null){
//            myElement.setImage(image);
//
//            Point2D.Double d = null;
//            var res  = this.executor.submit(new ResultReturnerCallable(myElement, this.imageConverter, this.resultConsumer));
//            try {
//                d = (Point2D.Double) res.get();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (ExecutionException e) {
//                e.printStackTrace();
//            }
            //returnList.set(frameNumber, d);

//                try {
//                    res.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }

//        }else{
//            synchronized (elementsList){
//                this.elementsList.add(new ElementToProcess(frameNumber, image));
//            }
//        }
    }

    private class ResultReturnerCallable implements Callable{
        private Integer frameNumber;
        private  int[][] firstImage = null;
        private int[][] secondImage = null;
        ImageConverterInterface imageConverterInterface = null;
        ResultConsumerInterface resultConsumerInterface = null;

        public ResultReturnerCallable(Integer frameNumber, int[][] firstImage, int[][] secondImage, ImageConverterInterface imageConverterInterface, ResultConsumerInterface resultConsumer) {
            this.frameNumber = frameNumber;
            this.firstImage = firstImage;
            this.secondImage = secondImage;
            this.imageConverterInterface = imageConverterInterface;
            this.resultConsumerInterface = resultConsumer;
        }
        public int getFrameNumber() {
            return frameNumber;
        }
        public int getPriority() {
            return this.frameNumber ;
        }

        @Override
        public Object call() throws Exception {
            Point2D.Double point2D = null;
            point2D = imageConverterInterface.convert(frameNumber, firstImage, secondImage);
            executor2.execute(new ResultWrapper(point2D, frameNumber, resultConsumerInterface));
            return point2D;
        }
    }
}
