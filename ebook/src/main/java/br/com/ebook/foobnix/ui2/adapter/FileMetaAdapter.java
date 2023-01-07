package br.com.ebook.foobnix.ui2.adapter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.com.ebook.foobnix.android.utils.Dips;
import br.com.ebook.foobnix.android.utils.ResultResponse;
import br.com.ebook.foobnix.android.utils.TxtUtils;
import br.com.ebook.foobnix.dao2.FileMeta;
import br.com.ebook.foobnix.pdf.info.TintUtil;
import br.com.ebook.foobnix.pdf.info.wrapper.AppState;
import br.com.ebook.foobnix.pdf.info.wrapper.PopupHelper;
import br.com.ebook.foobnix.ui2.AppDB;
import br.com.ebook.foobnix.ui2.AppRecycleAdapter;
import br.com.ebook.foobnix.ui2.fast.FastScroller;
import br.com.ebook.universalimageloader.core.listener.SimpleImageLoadingListener;
import br.com.ebook.R;
import br.com.ebook.foobnix.android.utils.ResultResponse2;
import br.com.ebook.foobnix.pdf.info.IMG;

public class FileMetaAdapter extends AppRecycleAdapter<FileMeta, RecyclerView.ViewHolder> implements FastScroller.SectionIndexer {

    public static final int DISPLAY_TYPE_FILE = 2;
    public static final int DISPLAY_TYPE_DIRECTORY = 3;
    public static final int DISPALY_TYPE_LAYOUT_STARS = 4;

    public static final int DISPALY_TYPE_LAYOUT_TITLE_FOLDERS = 5;
    public static final int DISPALY_TYPE_LAYOUT_TITLE_BOOKS = 6;
    public static final int DISPALY_TYPE_SERIES = 7;
    public static final int DISPALY_TYPE_LAYOUT_TITLE_NONE = 8;

    public static final int ADAPTER_LIST = 0;
    public static final int ADAPTER_GRID = 1;
    public static final int ADAPTER_COVERS = 3;
    public static final int ADAPTER_LIST_COMPACT = 4;

    private int adapterType = ADAPTER_LIST;

    public static final int TEMP_VALUE_NONE = 0;
    public static final int TEMP_VALUE_FOLDER_PATH = 1;
    public static final int TEMP_VALUE_STAR_GRID_ITEM = 2;
    public int tempValue = TEMP_VALUE_NONE;

    public class FileMetaViewHolder extends RecyclerView.ViewHolder {
        public TextView title, author, path, browserExt, size, date, series, idPercentText;
        public ImageView image, star, menu;
        public View authorParent, progresLayout, parent, remove, layoutBootom, infoLayout, idProgressColor, idProgressBg, imageParent;

        public FileMetaViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.title1);
            author = (TextView) view.findViewById(R.id.title2);
            authorParent = view.findViewById(R.id.title2Parent);
            path = (TextView) view.findViewById(R.id.browserPath);
            size = (TextView) view.findViewById(R.id.browserSize);
            browserExt = (TextView) view.findViewById(R.id.browserExt);
            date = (TextView) view.findViewById(R.id.browseDate);
            series = (TextView) view.findViewById(R.id.series);
            idPercentText = (TextView) view.findViewById(R.id.idPercentText);

            image = (ImageView) view.findViewById(R.id.browserItemIcon);
            star = (ImageView) view.findViewById(R.id.starIcon);
            idProgressColor = view.findViewById(R.id.idProgressColor);
            idProgressBg = view.findViewById(R.id.idProgressBg);
            infoLayout = view.findViewById(R.id.infoLayout);
            imageParent = view.findViewById(R.id.imageParent);

            progresLayout = view.findViewById(R.id.progresLayout);
            layoutBootom = view.findViewById(R.id.layoutBootom);

            menu = (ImageView) view.findViewById(R.id.itemMenu);
            remove = view.findViewById(R.id.delete);

            parent = view;
        }
    }

    public class DirectoryViewHolder extends RecyclerView.ViewHolder {
        public TextView title, path;
        public ImageView image, starIcon;
        public View parent;

        public DirectoryViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.text1);
            path = (TextView) view.findViewById(R.id.text2);
            image = (ImageView) view.findViewById(R.id.image1);
            starIcon = (ImageView) view.findViewById(R.id.starIcon);
            parent = view;
        }
    }

    public class StarsLayoutViewHolder extends RecyclerView.ViewHolder {
        public RecyclerView recyclerView;
        public TextView clearAllRecent, clearAllStars, starredName, recentName;
        public View panelStars, panelRecent;

        public StarsLayoutViewHolder(View view) {
            super(view);
            recyclerView = (RecyclerView) view.findViewById(R.id.recyclerViewStars);
            starredName = (TextView) view.findViewById(R.id.starredName);
            recentName = (TextView) view.findViewById(R.id.recentName);
            panelStars = view.findViewById(R.id.panelStars);
            panelRecent = view.findViewById(R.id.panelRecent);
        }
    }

    public class StarsTitleViewHolder extends RecyclerView.ViewHolder {
        public TextView clearAllFolders, clearAllBooks;
        public View parent;
        public ImageView onGridList;

        public StarsTitleViewHolder(View view) {
            super(view);
            clearAllFolders = (TextView) view.findViewById(R.id.clearAllFolders);
            clearAllBooks = (TextView) view.findViewById(R.id.clearAllBooks);
            onGridList = (ImageView) view.findViewById(R.id.onGridList);
            parent = view.findViewById(R.id.parent);
        }
    }

    public class NoneHolder extends RecyclerView.ViewHolder {

        public NoneHolder(View view) {
            super(view);
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        if (viewType == DISPALY_TYPE_LAYOUT_STARS) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_stars, parent, false);
            return new StarsLayoutViewHolder(itemView);
        }

        if (viewType == DISPALY_TYPE_LAYOUT_TITLE_FOLDERS) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_starred_title_folders, parent, false);
            return new StarsTitleViewHolder(itemView);
        }

        if (viewType == DISPALY_TYPE_LAYOUT_TITLE_BOOKS) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_starred_title_books, parent, false);
            return new StarsTitleViewHolder(itemView);
        }
        if (viewType == DISPALY_TYPE_LAYOUT_TITLE_NONE) {
            itemView = new View(parent.getContext());
            return new NoneHolder(itemView);
        }

        if (viewType == DISPLAY_TYPE_DIRECTORY) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browse_dir, parent, false);
            return new DirectoryViewHolder(itemView);
        }

        if (viewType == DISPLAY_TYPE_DIRECTORY) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browse_dir, parent, false);
            return new DirectoryViewHolder(itemView);
        }

        if (viewType == DISPLAY_TYPE_FILE) {
            if (adapterType == ADAPTER_LIST || adapterType == ADAPTER_LIST_COMPACT) {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browse_item_list, parent, false);
            } else {
                itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.browse_item_grid, parent, false);
                if (tempValue == TEMP_VALUE_STAR_GRID_ITEM) {
                    itemView.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
                    // itemView.getLayoutParams().height = itemView.getLayoutParams().width * 2;
                }
            }
            return new FileMetaViewHolder(itemView);
        }
        return null;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holderAll) {
//        super.onViewRecycled(holderAll);
//        if (holderAll instanceof FileMetaViewHolder) {
//            final FileMetaViewHolder holder = (FileMetaViewHolder) holderAll;
//            ImageLoader.getInstance().cancelDisplayTask(holder.image);
//            LOG.d("onViewRecycled");
//        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holderAll, final int position) {
        final FileMeta fileMeta = getItem(position);

        if (holderAll instanceof StarsTitleViewHolder) {
            final StarsTitleViewHolder holder = (StarsTitleViewHolder) holderAll;
            if (holder.clearAllFolders != null) {
                TxtUtils.underlineTextView(holder.clearAllFolders).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        clearAllStarredFolders.run();
                    }
                });
            }

            if (holder.clearAllBooks != null) {
                TxtUtils.underlineTextView(holder.clearAllBooks).setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        clearAllStarredBooks.run();
                    }
                });
            }

            if (holder.onGridList != null) {
                PopupHelper.updateGridOrListIcon(holder.onGridList, AppState.get().starsMode);
                holder.onGridList.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onGridOrList.onResultRecive(holder.onGridList);
                    }
                });
            }

            TintUtil.setBackgroundFillColor(holder.parent, TintUtil.color);
        }
        if (holderAll instanceof FileMetaViewHolder) {

            final FileMetaViewHolder holder = (FileMetaViewHolder) holderAll;

            if (!AppState.get().isShowImages && adapterType == ADAPTER_COVERS) {
                adapterType = ADAPTER_GRID;
            }

            bindFileMetaView(holder, position);

            IMG.getCoverPageWithEffect(holder.image, fileMeta.getPath(), IMG.getImageSize(), new SimpleImageLoadingListener() {

                @Override
                public void onLoadingCancelled(String imageUri, View view) {
                    super.onLoadingCancelled(imageUri, view);
                }

                @Override
                public void onLoadingStarted(String imageUri, View view) {
                    super.onLoadingStarted(imageUri, view);
                }

                @Override
                public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
                    if (position <= items.size() - 1) {
                        items.set(position, AppDB.get().getOrCreate(fileMeta.getPath()));
                        bindFileMetaView(holder, position);
                    }
                }

            });

            holder.imageParent.setVisibility(AppState.get().isShowImages ? View.VISIBLE : View.GONE);

        } else if (holderAll instanceof DirectoryViewHolder) {
            final DirectoryViewHolder holder = (DirectoryViewHolder) holderAll;
            holder.title.setText(fileMeta.getPathTxt());
            holder.path.setText(fileMeta.getPath());

            TintUtil.setTintImageWithAlpha(holder.image);
            bindItemClickAndLongClickListeners(holder.parent, fileMeta);
            if (!AppState.get().isBorderAndShadow) {
                holder.parent.setBackgroundColor(Color.TRANSPARENT);
            }

            if (AppDB.get().isStarFolder(fileMeta.getPath())) {
                holder.starIcon.setImageResource(R.drawable.star_1);
            } else {
                holder.starIcon.setImageResource(R.drawable.star_2);
            }
            TintUtil.setTintImageWithAlpha(holder.starIcon, TintUtil.color);

            if (onStarClickListener != null) {
                holder.starIcon.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        onStarClickListener.onResultRecive(fileMeta, FileMetaAdapter.this);
                    }
                });
            }
            if (adapterType == ADAPTER_GRID || adapterType == ADAPTER_COVERS) {
                holder.image.setVisibility(View.GONE);
                holder.path.setVisibility(View.GONE);
            } else {
                holder.image.setVisibility(View.VISIBLE);
                if (tempValue == TEMP_VALUE_FOLDER_PATH) {
                    holder.path.setVisibility(View.VISIBLE);
                } else {
                    holder.path.setVisibility(View.GONE);
                }
            }

        } else if (holderAll instanceof StarsLayoutViewHolder) {
            final StarsLayoutViewHolder holder = (StarsLayoutViewHolder) holderAll;
            FileMetaAdapter adapter = new FileMetaAdapter();
            adapter.setOnItemClickListener(onItemClickListener);
            adapter.setOnItemLongClickListener(onItemLongClickListener);

            adapter.setOnMenuClickListener(onMenuClickListener);
            adapter.setOnStarClickListener(onStarClickListener);

            adapter.setOnAuthorClickListener(onAuthorClickListener);
            adapter.setOnSeriesClickListener(onSeriesClickListener);

            adapter.setAdapterType(FileMetaAdapter.ADAPTER_GRID);
            adapter.tempValue = TEMP_VALUE_STAR_GRID_ITEM;
            holder.recyclerView.setAdapter(adapter);

            adapter.getItemsList().clear();
            List<FileMeta> allStars = AppDB.get().getStarsFiles();
            adapter.getItemsList().addAll(allStars);
            adapter.notifyDataSetChanged();

            TintUtil.setBackgroundFillColor(holder.panelRecent, TintUtil.color);
            TintUtil.setBackgroundFillColor(holder.panelStars, TintUtil.color);

            holder.starredName.setText(holder.starredName.getContext().getString(R.string.starred) + " (" + allStars.size() + ")");
            holder.recentName.setText(holder.starredName.getContext().getString(R.string.recent) + " (" + (getItemCount() - 1) + ")");

        }

    }

    private FileMeta bindFileMetaView(final FileMetaViewHolder holder, final int position) {
        if (position >= items.size()) {
            return new FileMeta();
        }
        final FileMeta fileMeta = getItem(position);

        holder.title.setText(fileMeta.getTitle());

        holder.author.setText(fileMeta.getAuthor());
        if (AppState.get().isInkMode) {
            holder.author.setTextColor(Color.BLACK);
            if (holder.series != null) {
                holder.series.setTextColor(Color.BLACK);
            }
        }

        if (TxtUtils.isEmpty(fileMeta.getAuthor())) {
            if (adapterType == ADAPTER_GRID) {
                holder.author.setVisibility(View.INVISIBLE);
            } else {
                holder.author.setVisibility(View.GONE);
            }
        } else {
            holder.author.setVisibility(View.VISIBLE);
        }

        if (holder.series != null && onSeriesClickListener != null) {
            String sequence = fileMeta.getSequence();
            holder.series.setVisibility(TxtUtils.isNotEmpty(sequence) ? View.VISIBLE : View.GONE);
            holder.series.setText(sequence);
            holder.series.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    if (onSeriesClickListener != null) {
                        onSeriesClickListener.onResultRecive(fileMeta.getSequence());
                    }
                }
            });
        }

        holder.author.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onAuthorClickListener != null) {
                    onAuthorClickListener.onResultRecive(fileMeta.getAuthor());
                }

            }
        });

        holder.path.setText(fileMeta.getPathTxt());
        holder.browserExt.setText(fileMeta.getChild() != null ? fileMeta.getChild() : fileMeta.getExt());
        holder.size.setText(fileMeta.getSizeTxt());
        if (holder.date != null) {
            holder.date.setText(fileMeta.getDateTxt());
        }

        double recentProgress = fileMeta.getIsRecentProgress();

        if (holder.idProgressColor != null && recentProgress > 0) {
            holder.progresLayout.setVisibility(View.VISIBLE);
            holder.idPercentText.setVisibility(View.VISIBLE);
            holder.idProgressColor.setBackgroundColor(TintUtil.color);
            int width = adapterType == ADAPTER_LIST_COMPACT ? Dips.dpToPx(100) : Dips.dpToPx(200);

            holder.idProgressBg.getLayoutParams().width = width;
            holder.idProgressColor.getLayoutParams().width = (int) (width * recentProgress);
            holder.idProgressColor.setLayoutParams(holder.idProgressColor.getLayoutParams());
            holder.idPercentText.setText("" + Math.round(100f * recentProgress) + "%");

        } else if (holder.progresLayout != null) {
            holder.progresLayout.setVisibility(View.INVISIBLE);
            holder.idPercentText.setVisibility(View.INVISIBLE);
        }

        if (adapterType == ADAPTER_GRID && recentProgress > 0) {
            holder.idPercentText.setText("" + (int) (100 * recentProgress) + "%");
            if (AppState.get().coverBigSize < IMG.TWO_LINE_COVER_SIZE) {
                holder.browserExt.setVisibility(View.GONE);
            } else {
                holder.browserExt.setVisibility(View.VISIBLE);
            }
        } else if (adapterType == ADAPTER_GRID) {
            holder.idPercentText.setText("");
            holder.browserExt.setVisibility(View.VISIBLE);
        }

        if (fileMeta.getIsStar() == null || fileMeta.getIsStar() == false) {
            holder.star.setImageResource(R.drawable.star_2);
        } else {
            holder.star.setImageResource(R.drawable.star_1);
        }
        TintUtil.setTintImageWithAlpha(holder.star, TintUtil.color);

        if (onStarClickListener != null) {
            holder.star.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    onStarClickListener.onResultRecive(fileMeta, FileMetaAdapter.this);
                }
            });
        } else {
        }

        bindItemClickAndLongClickListeners(holder.parent, fileMeta);

        if (adapterType == ADAPTER_GRID || adapterType == ADAPTER_COVERS) {
            holder.path.setVisibility(View.GONE);
            holder.size.setVisibility(View.GONE);

            int sizeDP = AppState.get().coverBigSize;
            if (tempValue == TEMP_VALUE_STAR_GRID_ITEM) {
                sizeDP = Math.max(80, AppState.get().coverSmallSize);
            }

            IMG.updateImageSizeBig((View) holder.image.getParent().getParent(), sizeDP);

            LayoutParams lp = holder.image.getLayoutParams();
            lp.width = Dips.dpToPx(sizeDP);

            if (AppState.get().isCropBookCovers) {
                lp.height = (int) (lp.width * IMG.WIDTH_DK);
            } else {
                lp.width = LayoutParams.WRAP_CONTENT;
                lp.height = LayoutParams.WRAP_CONTENT;
            }

        } else {
            holder.path.setVisibility(View.VISIBLE);
            holder.size.setVisibility(View.VISIBLE);

            IMG.updateImageSizeSmall((View) holder.image.getParent().getParent());

            LayoutParams lp = holder.image.getLayoutParams();
            lp.width = Dips.dpToPx(AppState.get().coverSmallSize);
            if (AppState.get().isCropBookCovers) {
                lp.height = (int) (lp.width * IMG.WIDTH_DK);
            } else {
                lp.height = LayoutParams.WRAP_CONTENT;
            }
        }
        if (holder.date != null) {
            holder.date.setVisibility(View.VISIBLE);
            holder.size.setVisibility(View.VISIBLE);
            if (adapterType == ADAPTER_LIST_COMPACT) {
                holder.date.setVisibility(View.GONE);
                holder.size.setVisibility(View.GONE);
            }
        }

        if (AppState.get().isBorderAndShadow) {
            View parent = (View) holder.image.getParent();
            parent.setBackgroundColor(Color.TRANSPARENT);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) parent.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, 0);
        }

        if (AppState.get().isCropBookCovers) {
            holder.image.setScaleType(ScaleType.CENTER_CROP);
        } else {
            holder.image.setScaleType(ScaleType.FIT_CENTER);
        }

        if (holder.layoutBootom != null) {
            if (adapterType == ADAPTER_COVERS) {
                holder.layoutBootom.setVisibility(View.GONE);
                holder.infoLayout.setVisibility(View.GONE);
            } else {
                holder.layoutBootom.setVisibility(View.VISIBLE);
                holder.infoLayout.setVisibility(View.VISIBLE);
            }
        }
        holder.authorParent.setVisibility(View.VISIBLE);
        if (adapterType == ADAPTER_LIST || adapterType == ADAPTER_LIST_COMPACT) {
            if (AppState.get().coverSmallSize >= IMG.TWO_LINE_COVER_SIZE) {
                holder.title.setSingleLine(false);
                holder.title.setLines(2);
                holder.path.setVisibility(View.VISIBLE);
                holder.title.setTextSize(16);
            } else {
                holder.title.setSingleLine(false);
                holder.title.setLines(2);
                holder.title.setTextSize(14);
                holder.authorParent.setVisibility(View.GONE);
                holder.path.setVisibility(View.GONE);
                holder.infoLayout.setVisibility(View.VISIBLE);
                holder.title.setText(fileMeta.getPathTxt());
            }
        }

        TintUtil.setTintImageWithAlpha(holder.menu);

        if (holder.remove != null) {
            holder.remove.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    onDeleteClickListener.onResultRecive(fileMeta);
                }
            });

            if (onDeleteClickListener == null) {
                holder.remove.setVisibility(View.GONE);
            }
        }

        holder.menu.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (onMenuClickListener != null) {
                    onMenuClickListener.onResultRecive(fileMeta);
                }
            }

        });
        holder.parent.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onItemClickListener.onResultRecive(fileMeta);
            }
        });
        holder.parent.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                onItemLongClickListener.onResultRecive(fileMeta);
                return true;
            }
        });
        if (!AppState.get().isBorderAndShadow) {
            holder.parent.setBackgroundColor(Color.TRANSPARENT);
        }

        return fileMeta;
    }

    @Override
    public String getSectionText(int position) {
        return " " + (position + 1) + " ";
    }

    @Override
    public int getItemViewType(int position) {
        FileMeta item = getItem(position);
        if (item == null) {
            return DISPLAY_TYPE_FILE;
        }
        Integer cusType = item.getCusType();
        if (cusType == null) {
            return DISPLAY_TYPE_FILE;
        }
        return cusType;
    }

    public void setOnMenuClickListener(ResultResponse<FileMeta> onMenuClickListener) {
        this.onMenuClickListener = onMenuClickListener;
    }

    public void setOnDeleteClickListener(ResultResponse<FileMeta> onDeleteClickListener) {
        this.onDeleteClickListener = onDeleteClickListener;
    }

    public void setOnAuthorClickListener(ResultResponse<String> onAuthorClickListener) {
        this.onAuthorClickListener = onAuthorClickListener;
    }

    public void setOnSeriesClickListener(ResultResponse<String> onSeriesClickListener) {
        this.onSeriesClickListener = onSeriesClickListener;
    }

    public void setAdapterType(int adapterType) {
        this.adapterType = adapterType;
    }

    public void setOnStarClickListener(ResultResponse2<FileMeta, FileMetaAdapter> onStarClickListener) {
        this.onStarClickListener = onStarClickListener;
    }

    public void setClearAllStarredFolders(Runnable clearAllStarredFolders) {
        this.clearAllStarredFolders = clearAllStarredFolders;
    }

    public void setClearAllStarredBooks(Runnable clearAllStarredBooks) {
        this.clearAllStarredBooks = clearAllStarredBooks;
    }

    public void setOnGridOrList(ResultResponse<ImageView> onGridOrList) {
        this.onGridOrList = onGridOrList;
    }

    private ResultResponse<FileMeta> onMenuClickListener;
    private ResultResponse<FileMeta> onDeleteClickListener;
    private ResultResponse<String> onAuthorClickListener;
    private ResultResponse<String> onSeriesClickListener;
    private ResultResponse2<FileMeta, FileMetaAdapter> onStarClickListener;
    private Runnable clearAllStarredFolders;
    private Runnable clearAllStarredBooks;
    private ResultResponse<ImageView> onGridOrList;

}