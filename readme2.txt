import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;



public class MotionDetectionSystem implements MotionDetectionSystemInterface {

    private ThreadPoolExecutor executor;
    private ThreadPoolExecutor executor2;

    private ImageConverterInterface imageConverter;
    private ResultConsumerInterface resultConsumer;
    //private ConcurrentSkipListSet<ElementToProcess> elementsList = new ConcurrentSkipListSet<>();
    private ArrayList<ElementToProcess> elementsList = new ArrayList<>(100);

    public AtomicInteger returnedCounter = new AtomicInteger(-1);

    public static int GetChunk(int num){
        double s = num / 2.0;
        String dbl = Double.toString(s);
        String res = dbl.substring(0,dbl.indexOf("."));
        return Integer.valueOf(res);
    }

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
            while (frameNumber != returnedCounter.intValue() + 1){
                try {
                    Thread.sleep(1);
                    //this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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


            BlockingQueue<Runnable> priorityQueue = new ArrayBlockingQueue<Runnable>(1000);
            executor = new ThreadPoolExecutor(0, 10, 0L, TimeUnit.MILLISECONDS,
                    new PriorityBlockingQueue<Runnable>(1000, new PriorityFutureComparator())) {
                protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
                    RunnableFuture<T> newTaskFor = super.newTaskFor(callable);
                    return new PriorityFuture<T>(newTaskFor, ((ResultReturnerCallable) callable).getPriority());
                }
            };
            //new ThreadPoolExecutor(0, 10, 10000, TimeUnit.MILLISECONDS, priorityQueue, new RejectedExecutionHandlerImpl());
            //(10000, 10000, 10000, TimeUnit.MILLISECONDS, priorityQueue);
            //(ThreadPoolExecutor)Executors.newCachedThreadPool();

            executor.setMaximumPoolSize(10);
            executor.setCorePoolSize(0);
            //executor.prestartAllCoreThreads();
            //executor.setQ
            //new ThreadPoolExecutor(10000, 10000, 100, TimeUnit.MILLISECONDS, priorityQueue); //TODO: ogarnąć KeepAlive
            executor2 = (ThreadPoolExecutor)Executors.newCachedThreadPool();
            executor2.setCorePoolSize(100);
            //executor.setCorePoolSize( 100);
            //
        }

        executor.setCorePoolSize(threads);
        executor.setMaximumPoolSize(threads);
        //executor.setKeepAliveTime(1000,TimeUnit.MICROSECONDS);

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

//        Runnable task = new Runnable(){
//
//            @Override
//            public void run(){
//                System.out.println("Task #1 is running");
//            }
//        };

//        try {
//            Thread.sleep(20);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }


        int myElementNumber = GetChunk(frameNumber);
        ElementToProcess myElement = null;
        synchronized (elementsList) {
            myElement = this.elementsList.stream().filter(elementToProcess -> elementToProcess.elementNumber == myElementNumber).findFirst().orElse(null);
            //this.elementsList.remove(myElement);
        }
        if(myElement != null){
            myElement.setImage(image);
            //synchronized (executor){
            //RunnableFuture res = (RunnableFuture)

            this.executor.submit(new ResultReturnerCallable(myElement, this.imageConverter, this.resultConsumer));


//                try {
//                    res.wait();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            // }
        }else{
            synchronized (elementsList){
                this.elementsList.add(new ElementToProcess(frameNumber, image));
            }

        }
    }

    private class ResultReturnerCallable implements Callable{ //Runnable, , Comparable<Runnable>

        private final Integer frameNumber;
        ElementToProcess elementToProcess = null;
        ImageConverterInterface imageConverterInterface = null;
        ResultConsumerInterface resultConsumerInterface = null;

        public int getFrameNumber() {
            return frameNumber;
        }

        private ResultReturnerCallable(ElementToProcess elementToProcess, ImageConverterInterface imageConverterInterface, ResultConsumerInterface resultConsumerInterface) {
            this.frameNumber = (elementToProcess.firstFrameNumber == null) ? GetChunk(elementToProcess.secondFrameNumber) : GetChunk(elementToProcess.firstFrameNumber);;
            this.elementToProcess = elementToProcess;
            this.imageConverterInterface = imageConverterInterface;
            this.resultConsumerInterface = resultConsumerInterface;
        }

//        @Override
//        public void run() {
//            Point2D.Double point2D = null;
//            //synchronized (imageConverterInterface){
//            point2D = imageConverterInterface.convert(frameNumber, elementToProcess.firstImage, elementToProcess.secondImage);
//            //}
//            //synchronized (resultConsumerInterface){
//            executor2.submit(new ResultWrapper(point2D, this.frameNumber, this.resultConsumerInterface));
//                 //resultConsumerInterface.accept(frameNumber, point2D);
//            //}
//
//
//        }
//        @Override
//        public int compareTo(ResultReturnerRunnable o) {
//            final long diff = o.frameNumber - frameNumber;
//            return 0 == diff ? 0 : 0 > diff ? -1 : 1;
//            //return this.elementNumber.compareTo(o.elementNumber);
//        }
//
//        @Override
//        public int compareTo(Runnable o) {
//            return this.frameNumber.compareTo(((ResultReturnerCallable)o).frameNumber);
//        }

        public int getPriority() {
            return this.frameNumber ;
        }

        @Override
        public Object call() throws Exception {
            Point2D.Double point2D = null;
            //synchronized (imageConverterInterface){
            point2D = imageConverterInterface.convert(frameNumber, elementToProcess.firstImage, elementToProcess.secondImage);
            //}
            //synchronized (resultConsumerInterface){
            executor2.submit(new ResultWrapper(point2D, this.frameNumber, this.resultConsumerInterface));

            return point2D;
        }
//        @Override
//        public int compareTo(ResultReturnerRunnable o) {
//            return this.frameNumber.compareTo(o.frameNumber);
//        }
    }
}
