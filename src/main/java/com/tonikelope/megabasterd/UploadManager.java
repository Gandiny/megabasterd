package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.*;
import java.awt.Component;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import static java.util.logging.Level.SEVERE;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public final class UploadManager extends TransferenceManager {

    private final ConcurrentLinkedQueue<Upload> _finishing_uploads_queue;

    private final Object _log_file_lock;

    public UploadManager(MainPanel main_panel) {

        super(main_panel, main_panel.getMax_ul(), main_panel.getView().getStatus_up_label(), main_panel.getView().getjPanel_scroll_up(), main_panel.getView().getClose_all_finished_up_button(), main_panel.getView().getPause_all_up_button(), main_panel.getView().getClean_all_up_menu());
        _finishing_uploads_queue = new ConcurrentLinkedQueue<>();

        _log_file_lock = new Object();
    }

    public Object getLog_file_lock() {
        return _log_file_lock;
    }

    public ConcurrentLinkedQueue<Upload> getFinishing_uploads_queue() {
        return _finishing_uploads_queue;
    }

    @Override
    public void provision(final Transference upload) {
        swingInvoke(
                new Runnable() {
            @Override
            public void run() {
                getScroll_panel().add(((Upload) upload).getView());
                ((Upload) upload).getView().revalidate();
                ((Upload) upload).getView().repaint();
            }
        });

        ((Upload) upload).provisionIt();

        if (((Upload) upload).isProvision_ok()) {

            increment_total_size(upload.getFile_size());

            getTransference_waitstart_queue().add(upload);

            synchronized (getQueue_sort_lock()) {

                if (!isPreprocessing_transferences() && !isProvisioning_transferences()) {

                    sortTransferenceStartQueue();

                    swingInvoke(
                            new Runnable() {
                        @Override
                        public void run() {

                            for (Transference up : getTransference_waitstart_queue()) {

                                getScroll_panel().remove((Component) up.getView());
                                getScroll_panel().add((Component) up.getView());
                                ((Upload) up).getView().revalidate();
                                ((Upload) up).getView().repaint();
                            }

                            for (Transference up : getTransference_finished_queue()) {

                                getScroll_panel().remove((Component) up.getView());
                                getScroll_panel().add((Component) up.getView());
                                ((Upload) up).getView().revalidate();
                                ((Upload) up).getView().repaint();
                            }
                        }
                    });

                }

            }

        } else {

            getTransference_finished_queue().add(upload);
        }

        secureNotify();
    }

    @Override
    public void remove(Transference[] uploads) {

        ArrayList<String[]> delete_up = new ArrayList<>();

        for (final Transference u : uploads) {

            swingInvoke(
                    new Runnable() {
                @Override
                public void run() {
                    getScroll_panel().remove(((Upload) u).getView());
                    ((Upload) u).getView().revalidate();
                    ((Upload) u).getView().repaint();
                }
            });

            getTransference_waitstart_queue().remove(u);

            getTransference_running_list().remove(u);

            getTransference_finished_queue().remove(u);

            increment_total_size(-1 * u.getFile_size());

            delete_up.add(new String[]{u.getFile_name(), ((Upload) u).getMa().getFull_email()});
        }

        try {
            DBTools.deleteUploads(delete_up.toArray(new String[delete_up.size()][]));
        } catch (SQLException ex) {
            Logger.getLogger(getClass().getName()).log(SEVERE, null, ex);
        }

        secureNotify();
    }

}
