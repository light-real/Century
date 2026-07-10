package com.bainian.memory.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bainian.memory.data.RecordRepository;
import com.bainian.memory.model.MemoryRecord;
import com.bainian.memory.util.UiKit;
import com.bainian.memory.util.WidgetFactory;
import com.bainian.memory.view.ImageGalleryView;
import com.bainian.memory.view.SettingsIconView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 记录卡片构建器：首页回忆流、日历页共用同一套卡片渲染逻辑。
 * 视觉上遵循「图片优先」原则——有图片的记录，图片在标题之前、且尽量大；
 * 卡片点击进入编辑，图片点击进入全屏查看，删除按钮触发确认弹窗。
 */
public final class RecordCardFactory {

    private RecordCardFactory() {}

    public interface Callbacks {
        void onEdit(LocalDate date, MemoryRecord record);
        void onDeleteRequested(MemoryRecord record);
        /** 标签等轻量信息发生变化后触发，用于刷新当前列表 */
        default void onRefresh() {}
    }

    public static View create(Context ctx, RecordRepository repo, MemoryRecord r, boolean allowDelete, Callbacks callbacks) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(UiKit.getDrawableByName(ctx, "card_elevated"));
        card.setLayoutParams(WidgetFactory.fullWidthMargin(ctx, UiKit.dp(ctx, 4), UiKit.dp(ctx, 14)));

        int pad = UiKit.dp(ctx, 18);
        List<String> imageUris = r.getImageUris();
        boolean hasImage = !imageUris.isEmpty();

        // 图片优先：有图先展示图片（贴边、不留 padding），再展示文字内容
        if (hasImage) {
            card.addView(ImageGalleryView.build(ctx, imageUris,
                    uri -> ImageViewerDialog.show(ctx, uri)));
        }

        LinearLayout textBlock = new LinearLayout(ctx);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setPadding(pad, hasImage ? UiKit.dp(ctx, 12) : UiKit.dp(ctx, 16), pad, UiKit.dp(ctx, 14));
        card.addView(textBlock);

        // 日期行：友好格式（今天/昨天/M月d日 星期几）+ 删除按钮
        LinearLayout dateRow = new LinearLayout(ctx);
        dateRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView dt = new TextView(ctx);
        dt.setText(friendlyDate(r.date));
        dt.setTextColor(UiKit.getColorByName(ctx, "accent"));
        dt.setTextSize(12);
        dt.setTypeface(Typeface.DEFAULT_BOLD);
        dateRow.addView(dt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (allowDelete) {
            SettingsIconView del = new SettingsIconView(ctx);
            del.setKind(SettingsIconView.Kind.CLOSE);
            del.setIconColor(UiKit.getColorByName(ctx, "muted_light"));
            int delSize = UiKit.dp(ctx, 12);
            LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(delSize, delSize);
            delLp.setMargins(UiKit.dp(ctx, 14), 0, UiKit.dp(ctx, 4), 0);
            delLp.gravity = Gravity.CENTER_VERTICAL;
            del.setLayoutParams(delLp);
            del.setOnClickListener(v -> callbacks.onDeleteRequested(r));
            dateRow.addView(del);
        }
        textBlock.addView(dateRow);

        if (!r.body.isEmpty()) {
            TextView body = new TextView(ctx);
            body.setText(r.body);
            body.setTextColor(UiKit.getColorByName(ctx, "ink"));
            body.setTextSize(15);
            body.setLineSpacing(UiKit.dp(ctx, 5), 1f);
            body.setPadding(0, UiKit.dp(ctx, 6), 0, 0);
            textBlock.addView(body);
        }

        // 标签在记录时不强制填写，这里提供事后补充的轻量入口；已有标签则直接展示
        LinearLayout chips = new LinearLayout(ctx);
        chips.setGravity(Gravity.LEFT);
        chips.setPadding(0, UiKit.dp(ctx, 6), 0, 0);
        if (!r.tag.isEmpty()) {
            chips.addView(WidgetFactory.chip(ctx, r.tag));
        } else {
            TextView addTag = new TextView(ctx);
            addTag.setText("+ 添加标签");
            addTag.setTextSize(12);
            addTag.setTextColor(UiKit.getColorByName(ctx, "muted_light"));
            addTag.setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 6), UiKit.dp(ctx, 12), UiKit.dp(ctx, 6));
            addTag.setOnClickListener(v -> RecordActions.editTag(ctx, repo, r, callbacks::onRefresh));
            chips.addView(addTag);
        }
        card.addView(chips);

        // 整卡点击编辑（删除按钮自身会消费点击事件，不冲突）
        card.setOnClickListener(v -> {
            try {
                callbacks.onEdit(LocalDate.parse(r.date, RecordRepository.DATE_KEY), r);
            } catch (Exception e) {
                callbacks.onEdit(LocalDate.now(), r);
            }
        });
        return card;
    }

    /** 把 ISO 日期转成更有人情味的展示：今天 / 昨天 / M月d日 星期几（跨年份加上年份） */
    private static String friendlyDate(String iso) {
        try {
            LocalDate d = LocalDate.parse(iso, RecordRepository.DATE_KEY);
            LocalDate today = LocalDate.now();
            if (d.equals(today)) return "今天";
            if (d.equals(today.minusDays(1))) return "昨天";
            String pattern = d.getYear() == today.getYear() ? "M月d日 EEEE" : "yyyy年M月d日 EEEE";
            return d.format(DateTimeFormatter.ofPattern(pattern, Locale.CHINA));
        } catch (Exception e) {
            return iso;
        }
    }
}
