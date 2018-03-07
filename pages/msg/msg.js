// pages/msg/msg.js
const app = getApp()
const ACTION_DIS = 50;
Page({

    /**
     * 页面的初始数据
     */
    data: {
        distance: 50,
        advice: false,
        list: [
            { avatar: "", name: "", desc: "", msg: 1 },
            { avatar: "", name: "", desc: "", msg: 0 },
            { avatar: "", name: "", desc: "", msg: 0 },
        ]
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad: function (options) {
        getApp().editTabBar();
    },
    onStart(evt) {
        const { list } = this.data
        const { index } = evt.currentTarget.dataset
        list.forEach((o, ind) => {
            index != ind && (o.pos = 0)
        })
        this.setData({ list })
        const [touch] = evt.touches
        this['pos' + index] = touch.pageX
    },
    onMove(evt) {
        const { list, distance } = this.data
        const { index } = evt.currentTarget.dataset
        const [touch] = evt.touches
        let dis = touch.pageX - this['pos' + index]
        if (dis >= 0) {
            dis = 0
        }
        if (dis <= -distance) {
            dis = -distance
        }

        this.setData({
            [`list[${index}].pos`]: dis
        })
    },
    onEnd(evt) {
        const { list, distance } = this.data
        const { index } = evt.currentTarget.dataset
        this.setData({
            [`list[${index}].pos`]: list[index].pos < -distance / 2 ? -distance : 0
        })
    },
    // 删除聊天
    delClick(evt) {
        const { list } = this.data
        const { index } = evt.currentTarget.dataset
        // 请求接口
        wx.showModal({
            title: '提示',
            content: '是否删除与',
            success: res => {
                if (res.confirm) {
                    console.log('用户点击确定')
                    list.splice(index, 1)
                    this.setData({
                        list
                    })
                } else if (res.cancel) {
                    console.log('用户点击取消')
                }
            }
        })
    },
    // 屏蔽聊天
    noClick(evt) {
        const { list } = this.data
        const { index } = evt.currentTarget.dataset
        // 请求接口
        wx.showModal({
            title: '提示',
            content: '屏蔽后，不再接收这个用户消息！',
            success: res => {
                if (res.confirm) {
                    console.log('用户点击确定')
                    list.splice(index, 1)
                    this.setData({
                        list
                    })
                } else if (res.cancel) {
                    console.log('用户点击取消')
                }
            }
        })
    },
    // 举报
    reportClick(evt) {
        const { list } = this.data
        const { index } = evt.currentTarget.dataset
        // 请求接口
        this.setData({
            advice: true
        })
    },
    onInput(evt) {
        this.setData({
            content: evt.detail.value
        })
    },
    submit() {
        if (!this.data.content) {
            app.wxToast.error('请输入举报内容~');
            return;
        }
        // 请求接口，提交举报内容

    },
    closeClick() {
        this.setData({
            advice: false
        })
    },
})