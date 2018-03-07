// pages/mine/mine.js
const app = getApp()
Page({

    /**
     * 页面的初始数据
     */
    data: {
        advice: false,
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad: function (options) {
        wx.login({
            success: res => {
                wx.getUserInfo({
                    success: info => {
                        this.setData({
                            user: info.userInfo
                        })
                        app.user = this.data.user
                    }
                })
            }
        })
        getApp().editTabBar();
    },
    loadData() {
        // 请求接口
    },
    onInput(evt) {
        this.setData({
            content: evt.detail.value
        })
    },
    submit() {
        if (!this.data.content) {
            app.wxToast.error('请输入您的意见或建议~');
            return;
        }
        // 请求接口，提交意见反馈

    },
    adviceClick() {
        this.setData({
            advice: true
        })
    },
    closeClick() {
        this.setData({
            advice: false
        })
    },
    call() {
        wx.makePhoneCall({
            phoneNumber: '18888888888'
        })
    }
})