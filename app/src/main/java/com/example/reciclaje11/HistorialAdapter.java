package com.example.reciclaje11;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.reciclaje11.databinding.ItemHistorialBinding;
import com.example.reciclaje11.db.ObjetoReciclado;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistorialAdapter extends RecyclerView.Adapter<HistorialAdapter.ViewHolder> {

    private List<ObjetoReciclado> items;
    private final OnItemLongClickListener longClickListener;

    public interface OnItemLongClickListener {
        void alMantenerPresionado(ObjetoReciclado item);
    }

    public HistorialAdapter(List<ObjetoReciclado> items, OnItemLongClickListener longClickListener) {
        this.items = items;
        this.longClickListener = longClickListener;
    }

    public void actualizarDatos(List<ObjetoReciclado> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistorialBinding binding = ItemHistorialBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ObjetoReciclado item = items.get(position);
        holder.binding.tvItemCategory.setText(item.categoria);
        holder.binding.tvItemLabel.setText(item.etiquetaImageLabeling != null ? "(" + item.etiquetaImageLabeling + ")" : "");
        holder.binding.tvItemConfidence.setText(String.format(Locale.getDefault(), "%.0f%%", item.confianza * 100));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        holder.binding.tvItemDate.setText(sdf.format(new Date(item.fechaEscaneo)));

        if (item.rutaImagen != null) {
            holder.binding.ivThumb.setImageBitmap(BitmapFactory.decodeFile(item.rutaImagen));
        }

        holder.itemView.setOnLongClickListener(v -> {
            longClickListener.alMantenerPresionado(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ItemHistorialBinding binding;
        ViewHolder(ItemHistorialBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
