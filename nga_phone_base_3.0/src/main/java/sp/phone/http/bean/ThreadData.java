package sp.phone.http.bean;

import java.util.List;

import com.alibaba.fastjson2.annotation.JSONField;

import gov.anzong.androidnga.common.base.JavaBean;
import sp.phone.mvp.model.entity.ThreadPageInfo;
import sp.phone.mvp.model.thread.ArticlePagingInfo;

public class ThreadData implements JavaBean {
    private List<ThreadRowInfo> rowList;
    private ThreadPageInfo threadInfo;
    private int __ROWS;
    /** Page the server actually rendered; 0 when the response did not report one. */
    private int __PAGE;
    /** Rows per page the server actually rendered with; 0 when the response did not report one. */
    private int __R__ROWS_PAGE;
    private int rowNum;
    private ArticlePagingInfo pagingInfo;
    private boolean contentComplete = true;

    public ArticlePagingInfo getPagingInfo() { return pagingInfo; }
    public void setPagingInfo(ArticlePagingInfo pagingInfo) { this.pagingInfo = pagingInfo; }
    public boolean isContentComplete() { return contentComplete; }
    public void setContentComplete(boolean complete) { contentComplete = complete; }

    /**
     * 从服务端获取的原始数据
     */
    private String mRawData;

    public List<ThreadRowInfo> getRowList() {
        return rowList;
    }

    public void setRowList(List<ThreadRowInfo> rowList) {
        this.rowList = rowList;
    }

    public ThreadPageInfo getThreadInfo() {
        return threadInfo;
    }

    public void setThreadInfo(ThreadPageInfo threadInfo) {
        this.threadInfo = threadInfo;
    }

    public int get__ROWS() {
        return __ROWS;
    }

    public void set__ROWS(int __ROWS) {
        this.__ROWS = __ROWS;
    }

    // Response-derived paging coordinates, not part of the legacy serialized bean shape.
    @JSONField(serialize = false, deserialize = false)
    public int get__PAGE() {
        return __PAGE;
    }

    @JSONField(serialize = false, deserialize = false)
    public void set__PAGE(int __PAGE) {
        this.__PAGE = __PAGE;
    }

    @JSONField(serialize = false, deserialize = false)
    public int get__R__ROWS_PAGE() {
        return __R__ROWS_PAGE;
    }

    @JSONField(serialize = false, deserialize = false)
    public void set__R__ROWS_PAGE(int __R__ROWS_PAGE) {
        this.__R__ROWS_PAGE = __R__ROWS_PAGE;
    }

    public int getRowNum() {
        return rowNum;
    }

    public void setRowNum(int rowNum) {
        this.rowNum = rowNum;
    }

    public String getRawData() {
        return mRawData;
    }

    public void setRawData(String rawData) {
        mRawData = rawData;
    }
}
