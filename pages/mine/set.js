// pages/mine/set.js
const app = getApp()
Page({

    /**
     * 页面的初始数据
     */
    data: {
        sex: [
            "男",
            "女"
        ],
        post: {}
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad: function (options) {
        this.setData({
            user: app.user
        })
    },
    bindPickerChange: function (e) {
        this.setData({
            index: e.detail.value
        })
    },
    choose() {
        wx.chooseImage({
            count: 1, // 默认9
            success: res => {
                const file = res.tempFilePaths[0];
                this.setData({
                    cover: file,
                })
            }
        })
    },
    onInput: function (evt) {
        var key = evt.currentTarget.dataset.key;
        var val = evt.detail.value;
        this.setData({
            [`post.${key}`]: val
        })
    },
    submit() {

    }
})