// pages/msg/detail.js
Page({

    /**
     * 页面的初始数据
     */
    data: {
        list: []
    },

    /**
     * 生命周期函数--监听页面加载
     */
    onLoad: function (options) {
        // setInterval(this.addMsg, 1500)
        for(let i=0;i<10;i++){this.addMsg()}
    },
    addMsg() {
        const i = Math.random()
        const { list } = this.data
        list.push({
            content: "首先页面每个item分为上下两层，上面一层放置正常内容，下面一层放置左滑显示出的按钮，这个可以使用z-index来实现分层",
            mine: i > .5
        })
        this.setData({ list })
    }
})