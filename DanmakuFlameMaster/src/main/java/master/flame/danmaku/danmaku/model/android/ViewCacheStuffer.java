package master.flame.danmaku.danmaku.model.android;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Interpolator;

import java.util.ArrayList;
import java.util.List;

import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDrawingCache;

public abstract class ViewCacheStuffer<VH extends ViewCacheStuffer.ViewHolder> extends BaseCacheStuffer {

    /**
     * 动画信息类，用于在 onBindViewHolder 中获取动画相关的时间信息
     */
    public static class AnimationInfo {
        /**
         * 弹幕播放进度，范围 0.0 ~ 1.0
         * 0.0 表示刚开始显示，1.0 表示即将消失
         */
        public final float progress;

        /**
         * 弹幕已播放的时间（毫秒）
         */
        public final long elapsedTime;

        /**
         * 弹幕的总持续时间（毫秒）
         */
        public final long duration;

        /**
         * 当前时间戳（毫秒）
         */
        public final long currentTime;

        public AnimationInfo(float progress, long elapsedTime, long duration, long currentTime) {
            this.progress = progress;
            this.elapsedTime = elapsedTime;
            this.duration = duration;
            this.currentTime = currentTime;
        }

        /**
         * 计算在指定时间段内的动画进度
         * @param startMs 动画开始时间（相对于弹幕出现）
         * @param durationMs 动画持续时间
         * @return 动画进度 0.0 ~ 1.0，超出范围则 clamp
         */
        public float getAnimationProgress(long startMs, long durationMs) {
            return getAnimationProgress(startMs, durationMs, null);
        }

        /**
         * 计算在指定时间段内的动画进度（带插值器）
         * @param startMs 动画开始时间（相对于弹幕出现）
         * @param durationMs 动画持续时间
         * @param interpolator 插值器，null 则使用线性插值
         * @return 动画进度 0.0 ~ 1.0
         */
        public float getAnimationProgress(long startMs, long durationMs, Interpolator interpolator) {
            if (elapsedTime < startMs) {
                return 0f;
            }
            if (durationMs <= 0) {
                return 1f;
            }
            float rawProgress = (float) (elapsedTime - startMs) / durationMs;
            rawProgress = Math.max(0f, Math.min(1f, rawProgress));
            if (interpolator != null) {
                return interpolator.getInterpolation(rawProgress);
            }
            return rawProgress;
        }

        /**
         * 根据进度在两个值之间插值
         * @param startMs 动画开始时间
         * @param durationMs 动画持续时间
         * @param fromValue 起始值
         * @param toValue 结束值
         * @return 插值后的值
         */
        public float animate(long startMs, long durationMs, float fromValue, float toValue) {
            return animate(startMs, durationMs, fromValue, toValue, null);
        }

        /**
         * 根据进度在两个值之间插值（带插值器）
         */
        public float animate(long startMs, long durationMs, float fromValue, float toValue, Interpolator interpolator) {
            float p = getAnimationProgress(startMs, durationMs, interpolator);
            return fromValue + (toValue - fromValue) * p;
        }

        /**
         * 循环动画，在两个值之间来回变化（无限循环）
         * @param cycleDurationMs 一个周期的时间（往返一次的时间）
         * @param fromValue 起始值
         * @param toValue 结束值
         * @return 当前值
         */
        public float animateCycle(long cycleDurationMs, float fromValue, float toValue) {
            if (cycleDurationMs <= 0) return fromValue;
            // 处理 elapsedTime 为负数的情况（如 seek 回退）
            long safeElapsedTime = Math.max(0, elapsedTime);
            long cyclePosition = safeElapsedTime % (cycleDurationMs * 2);
            float p;
            if (cyclePosition < cycleDurationMs) {
                p = (float) cyclePosition / cycleDurationMs;
            } else {
                p = 1f - (float) (cyclePosition - cycleDurationMs) / cycleDurationMs;
            }
            return fromValue + (toValue - fromValue) * p;
        }

        /**
         * 单次往返动画（先增后减），动画结束后保持在 fromValue。
         * 适用于点赞、心跳等一次性动画效果。
         *
         * 例如点赞动画：animateBounce(0, 250, 1.0f, 2.0f)
         * - 0-250ms: 从1.0放大到2.0
         * - 250-500ms: 从2.0缩小到1.0
         * - 500ms后: 保持1.0
         *
         * @param startMs 动画开始时间（相对于弹幕出现）
         * @param halfDurationMs 半程动画时间（总时长为 halfDurationMs * 2）
         * @param fromValue 起始值和结束值
         * @param peakValue 中间峰值
         * @return 当前值
         */
        public float animateBounce(long startMs, long halfDurationMs, float fromValue, float peakValue) {
            return animateBounce(startMs, halfDurationMs, fromValue, peakValue, null);
        }

        /**
         * 单次往返动画（带插值器）
         */
        public float animateBounce(long startMs, long halfDurationMs, float fromValue, float peakValue, Interpolator interpolator) {
            if (halfDurationMs <= 0) return fromValue;

            // 动画未开始
            if (elapsedTime < startMs) {
                return fromValue;
            }

            long totalDuration = halfDurationMs * 2;
            // 动画已结束，保持结束值
            if (elapsedTime >= startMs + totalDuration) {
                return fromValue;
            }

            long animTime = elapsedTime - startMs;
            float p;
            if (animTime < halfDurationMs) {
                // 前半程：从 fromValue 到 peakValue
                p = (float) animTime / halfDurationMs;
            } else {
                // 后半程：从 peakValue 到 fromValue
                p = 1f - (float) (animTime - halfDurationMs) / halfDurationMs;
            }

            if (interpolator != null) {
                p = interpolator.getInterpolation(p);
            }
            return fromValue + (peakValue - fromValue) * p;
        }

        /**
         * 判断动画是否已完成
         * @param startMs 动画开始时间
         * @param durationMs 动画持续时间
         * @return true 表示动画已完成
         */
        public boolean isAnimationFinished(long startMs, long durationMs) {
            return elapsedTime >= startMs + durationMs;
        }
    }

    /**
     * 动画变换类，用于在绘制缓存时应用动画效果
     */
    public static class AnimationTransform {
        public float scaleX = 1f;
        public float scaleY = 1f;
        public float rotation = 0f;
        public float alpha = 1f;
        public float pivotXRatio = 0.5f;  // 0~1，相对于弹幕宽度的比例
        public float pivotYRatio = 0.5f;  // 0~1，相对于弹幕高度的比例

        public AnimationTransform() {}

        public AnimationTransform(float scaleX, float scaleY, float rotation, float alpha) {
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.rotation = rotation;
            this.alpha = alpha;
        }

        /**
         * 是否有变换（非默认值）
         */
        public boolean hasTransformation() {
            return scaleX != 1f || scaleY != 1f || rotation != 0f || alpha != 1f;
        }

        /**
         * 重置为默认值
         */
        public void reset() {
            scaleX = 1f;
            scaleY = 1f;
            rotation = 0f;
            alpha = 1f;
            pivotXRatio = 0.5f;
            pivotYRatio = 0.5f;
        }
    }

    public static abstract class ViewHolder {

        public final View itemView;

        public ViewHolder(View itemView) {
            if (itemView == null) {
                throw new IllegalArgumentException("itemView may not be null");
            }
            this.itemView = itemView;
        }

        public void measure(int widthMeasureSpec, int heightMeasureSpec) {
            this.itemView.measure(widthMeasureSpec, heightMeasureSpec);
        }

        public int getMeasureWidth() {
            return this.itemView.getMeasuredWidth();
        }

        public int getMeasureHeight() {
            return this.itemView.getMeasuredHeight();
        }

        public void layout(int l, int t, int r, int b) {
            this.itemView.layout(l, t, r, b);
        }

        public void draw(Canvas canvas, AndroidDisplayer.DisplayerConfig displayerConfig) {
            float scaleX = itemView.getScaleX();
            float scaleY = itemView.getScaleY();
            float rotation = itemView.getRotation();
            float rotationX = itemView.getRotationX();
            float rotationY = itemView.getRotationY();
            float alpha = itemView.getAlpha();

            boolean hasTransformation = scaleX != 1f || scaleY != 1f || rotation != 0f
                    || rotationX != 0f || rotationY != 0f;

            int saveCount = canvas.save();
            try {
                if (hasTransformation) {
                    float pivotX = itemView.getPivotX();
                    float pivotY = itemView.getPivotY();
                    canvas.translate(pivotX, pivotY);
                    if (rotation != 0f) {
                        canvas.rotate(rotation);
                    }
                    if (scaleX != 1f || scaleY != 1f) {
                        canvas.scale(scaleX, scaleY);
                    }
                    canvas.translate(-pivotX, -pivotY);
                }

                if (alpha < 1f) {
                    int layerSaveCount = canvas.saveLayerAlpha(0, 0,
                            itemView.getWidth(), itemView.getHeight(),
                            (int) (alpha * 255));
                    this.itemView.draw(canvas);
                    canvas.restoreToCount(layerSaveCount);
                } else {
                    this.itemView.draw(canvas);
                }
            } finally {
                canvas.restoreToCount(saveCount);
            }
            //TODO: apply displayerConfig
        }
    }

    public static final int INVALID_TYPE = -1;
    public static final int MEASURE_VIEW_TYPE = -2;
    public static final int DRAW_VIEW_TYPE = -3;
    public static final int CACHE_VIEW_TYPE = -3;

    private final int mMaximumWidthPixels;
    private final int mMaximumHeightPixels;
    private SparseArray<List<VH>> mViewHolderArray = new SparseArray();

    public abstract VH onCreateViewHolder(int viewType);

    /**
     * 绑定弹幕数据到 ViewHolder。
     *
     * 【子View动画】如果需要对弹幕内的某个子View单独做动画：
     * 1. 重写 {@link #needDisableCache(BaseDanmaku, AnimationInfo)} 返回 true
     * 2. 在此方法中使用 animationInfo 对子View设置动画属性
     * 例如: viewHolder.mIcon.setRotation(animationInfo.animateCycle(1000, 0f, 360f));
     *
     * @param viewType 视图类型
     * @param viewHolder ViewHolder
     * @param danmaku 弹幕数据
     * @param animationInfo 动画信息，包含播放进度等。measure阶段为null，draw阶段不为null。
     * @param displayerConfig 显示配置
     * @param paint 画笔
     */
    public abstract void onBindViewHolder(int viewType, VH viewHolder, BaseDanmaku danmaku,
                                          AnimationInfo animationInfo,
                                          AndroidDisplayer.DisplayerConfig displayerConfig, TextPaint paint);

    /**
     * 【弹幕整体动画】对整个弹幕进行变换（缩放、旋转、透明度）。
     * 每帧绘制时都会调用此方法，即使使用缓存也会调用，性能较好。
     *
     * 如果需要对弹幕内的子View单独做动画，请使用 {@link #needDisableCache(BaseDanmaku, AnimationInfo)}
     * 配合 {@link #onBindViewHolder} 方法。
     *
     * @param animationInfo 动画信息，包含播放进度、已播放时间等
     * @param danmaku 弹幕数据
     * @param transform 变换对象，修改 scaleX/scaleY/rotation/alpha 等属性来应用动画
     */
    public void onTransformDanmaku(AnimationInfo animationInfo, BaseDanmaku danmaku, AnimationTransform transform) {
        // 默认不做任何变换，子类重写此方法来实现整体动画
    }

    /**
     * 判断弹幕是否需要禁用缓存以支持子 View 动画。
     * 返回 true 时，该弹幕每帧都会重新调用 onBindViewHolder 和 View.draw()，
     * 可以在 onBindViewHolder 中对子 View 设置动画属性。
     *
     * 提示：当动画结束后返回 false，可以恢复使用缓存，提高性能。
     * 例如：return !animationInfo.isAnimationFinished(0, 500);
     *
     * 注意事项：
     * 1. 动画结束后（返回 false），系统会使用缓存的 Bitmap，子View的变换会被忽略
     * 2. 如果动画结束时子View不是默认状态（如 scale != 1），需要在 onBindViewHolder 中
     *    判断动画是否结束并重置子View的变换属性，否则下次复用该 ViewHolder 时可能出现问题
     *
     * @param danmaku 弹幕数据
     * @param animationInfo 动画信息，可用于判断动画是否已结束
     * @return true 禁用缓存（支持子 View 动画），false 使用缓存（仅支持整体动画）
     */
    public boolean needDisableCache(BaseDanmaku danmaku, AnimationInfo animationInfo) {
        return false;
    }

    public int getItemViewType(int position, BaseDanmaku danmaku) {
        return 0;
    }

    public ViewCacheStuffer() {
        mMaximumWidthPixels = -1;  // FIXME: get maximum of canvas
        mMaximumHeightPixels = -1;
    }

    @Override
    public void measure(BaseDanmaku danmaku, TextPaint paint, boolean fromWorkerThread) {
        int viewType = getItemViewType(danmaku.index, danmaku);
        List<VH> viewHolders = mViewHolderArray.get(viewType);
        if (viewHolders == null) {
            viewHolders = new ArrayList<>();
            viewHolders.add(onCreateViewHolder(viewType));
            viewHolders.add(onCreateViewHolder(viewType));
            viewHolders.add(onCreateViewHolder(viewType));
            mViewHolderArray.put(viewType, viewHolders);
        }
        VH viewHolder = viewHolders.get(0);
        // measure 阶段不需要动画信息
        onBindViewHolder(viewType, viewHolder, danmaku, null, null, paint);
        // 使用 UNSPECIFIED 模式，让 View 自行决定尺寸
        int widthSpec = mMaximumWidthPixels > 0
                ? View.MeasureSpec.makeMeasureSpec(mMaximumWidthPixels, View.MeasureSpec.AT_MOST)
                : View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int heightSpec = mMaximumHeightPixels > 0
                ? View.MeasureSpec.makeMeasureSpec(mMaximumHeightPixels, View.MeasureSpec.AT_MOST)
                : View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        viewHolder.measure(widthSpec, heightSpec);
        int measuredWidth = viewHolder.getMeasureWidth();
        int measuredHeight = viewHolder.getMeasureHeight();
        // 只有在测量结果有效时才调用 layout
        if (measuredWidth > 0 && measuredHeight > 0) {
            viewHolder.layout(0, 0, measuredWidth, measuredHeight);
        }
        danmaku.paintWidth = measuredWidth;
        danmaku.paintHeight = measuredHeight;
    }

    @Override
    public void clearCaches() {

    }

    @Override
    public void releaseResource(BaseDanmaku danmaku) {
        super.releaseResource(danmaku);
        danmaku.tag = null;
    }

    // 用于缓存变换对象，避免频繁创建
    // 使用 ThreadLocal 保证线程安全（drawCache 可能在不同线程调用）
    private final ThreadLocal<AnimationTransform> mCacheTransformLocal = new ThreadLocal<AnimationTransform>() {
        @Override
        protected AnimationTransform initialValue() {
            return new AnimationTransform();
        }
    };
    private final ThreadLocal<Matrix> mCacheMatrixLocal = new ThreadLocal<Matrix>() {
        @Override
        protected Matrix initialValue() {
            return new Matrix();
        }
    };

    @Override
    public boolean drawCache(BaseDanmaku danmaku, Canvas canvas, float left, float top, Paint alphaPaint, TextPaint paint) {
        // 计算动画信息
        AnimationInfo animationInfo = createAnimationInfo(danmaku);

        // 如果需要禁用缓存（支持子View动画），返回false让系统调用drawDanmaku
        if (needDisableCache(danmaku, animationInfo)) {
            return false;
        }

        IDrawingCache<?> cache = danmaku.getDrawingCache();
        if (cache == null) {
            return false;
        }
        DrawingCacheHolder holder = (DrawingCacheHolder) cache.get();
        if (holder == null || holder.bitmap == null) {
            return false;
        }

        // 获取线程安全的变换对象
        AnimationTransform transform = mCacheTransformLocal.get();
        Matrix matrix = mCacheMatrixLocal.get();

        // 重置并获取变换参数
        transform.reset();
        onTransformDanmaku(animationInfo, danmaku, transform);

        // 如果没有变换，使用原始绘制方式
        if (!transform.hasTransformation()) {
            return holder.draw(canvas, left, top, alphaPaint);
        }

        // 有变换，需要应用矩阵
        float width = danmaku.paintWidth;
        float height = danmaku.paintHeight;
        float pivotX = left + width * transform.pivotXRatio;
        float pivotY = top + height * transform.pivotYRatio;

        int saveCount = canvas.save();
        try {
            // 应用变换
            matrix.reset();
            matrix.postTranslate(-pivotX, -pivotY);
            if (transform.rotation != 0f) {
                matrix.postRotate(transform.rotation);
            }
            if (transform.scaleX != 1f || transform.scaleY != 1f) {
                matrix.postScale(transform.scaleX, transform.scaleY);
            }
            matrix.postTranslate(pivotX, pivotY);
            canvas.concat(matrix);

            // 处理透明度
            Paint drawPaint = alphaPaint;
            if (transform.alpha < 1f) {
                if (drawPaint == null) {
                    drawPaint = paint;
                }
                if (drawPaint != null) {
                    int originalAlpha = drawPaint.getAlpha();
                    drawPaint.setAlpha((int) (originalAlpha * transform.alpha));
                    boolean result = holder.draw(canvas, left, top, drawPaint);
                    drawPaint.setAlpha(originalAlpha);
                    return result;
                } else {
                    // 没有 paint，使用 saveLayerAlpha
                    int layerCount = canvas.saveLayerAlpha(left, top, left + width, top + height,
                            (int) (transform.alpha * 255));
                    boolean result = holder.draw(canvas, left, top, null);
                    canvas.restoreToCount(layerCount);
                    return result;
                }
            }

            return holder.draw(canvas, left, top, drawPaint);
        } finally {
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * 根据弹幕状态创建动画信息
     */
    private AnimationInfo createAnimationInfo(BaseDanmaku danmaku) {
        DanmakuTimer timer = danmaku.getTimer();
        if (timer == null) {
            return new AnimationInfo(0f, 0, danmaku.getDuration(), 0);
        }
        long currentTime = timer.currMillisecond;
        long startTime = danmaku.getActualTime();
        long duration = danmaku.getDuration();
        long elapsedTime = currentTime - startTime;

        float progress = 0f;
        if (duration > 0) {
            progress = Math.max(0f, Math.min(1f, (float) elapsedTime / duration));
        }

        return new AnimationInfo(progress, elapsedTime, duration, currentTime);
    }

    @Override
    public void drawDanmaku(BaseDanmaku danmaku, Canvas canvas, float left, float top, boolean fromWorkerThread, AndroidDisplayer.DisplayerConfig displayerConfig) {
        int viewType = getItemViewType(danmaku.index, danmaku);
        List<VH> viewHolders = mViewHolderArray.get(viewType);
        VH viewHolder = null;
        if (viewHolders != null) {
            viewHolder = viewHolders.get(fromWorkerThread ? 1 : 2);
        }
        if (viewHolder == null) {
            return;
        }
        //ignore danmaku.padding, apply it onBindViewHolder
        displayerConfig.definePaintParams(fromWorkerThread);
        TextPaint paint = displayerConfig.getPaint(danmaku, fromWorkerThread);
        displayerConfig.applyPaintConfig(danmaku, paint, false);

        // 计算动画信息
        AnimationInfo animationInfo = createAnimationInfo(danmaku);
        onBindViewHolder(viewType, viewHolder, danmaku, animationInfo, displayerConfig, paint);
        viewHolder.measure(View.MeasureSpec.makeMeasureSpec(Math.round(danmaku.paintWidth), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(Math.round(danmaku.paintHeight), View.MeasureSpec.EXACTLY));
        boolean needRestore = false;
        if (!fromWorkerThread) {
            canvas.save();
            canvas.translate(left, top);
            needRestore = true;
        }
        // draw underline
        if (danmaku.underlineColor != 0) {
            Paint linePaint = displayerConfig.getUnderlinePaint(danmaku);
            float bottom = top + danmaku.paintHeight - displayerConfig.UNDERLINE_HEIGHT;
            canvas.drawLine(left, bottom, left + danmaku.paintWidth, bottom, linePaint);
        }
        //draw border
        if (danmaku.borderColor != 0) {
            Paint borderPaint = displayerConfig.getBorderPaint(danmaku);
            canvas.drawRect(left, top, left + danmaku.paintWidth, top + danmaku.paintHeight,
                    borderPaint);
        }
        //draw danmaku
        int layoutWidth = (int) danmaku.paintWidth;
        int layoutHeight = (int) danmaku.paintHeight;
        if (layoutWidth > 0 && layoutHeight > 0) {
            viewHolder.layout(0, 0, layoutWidth, layoutHeight);
            viewHolder.draw(canvas, displayerConfig); //FIXME: handle canvas.getMaximumBitmapWidth()
        }
        //TODO: stroke handle displayerConfig
        if (needRestore) {
            canvas.restore();
        }
    }

}