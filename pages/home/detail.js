const app = getApp()
Page({
    /**
     * 页面的初始数据
     */
    data: {
      detailObj:null,
      current: 1
    },
    onLoad: function (option) {
      //获取作品详情
      var that = this
      wx.request({
        url: app.apiUrl + '/api/getArtWorksDetail?id=' + option.id,
        success: function (e) {
          if (e.data.code == '0') {
            that.setData({
              detailObj: e.data.data
            })
          }
          console.log(that.data.detailObj)
        }
      })
    },
    currentClick(evt){
        this.setData({
            current: evt.currentTarget.dataset.current
        })
    },

    watch(evt){
        this.setData({
            "content.is_watch":!this.data.content.is_watch
        })
        //请求接口
    },

    /**
     * 用户点击右上角分享
     */
    onShareAppMessage: function () {
        return {
            title: "xxx",//分享名称
        }
    },
    like(evt){
        this.setData({
            "content.is_like": !this.data.content.is_like
        })
    },
    keep(evt){
        this.setData({
            "content.is_keep": !this.data.content.is_keep
        })
    },
    likeComment1(evt){
        const {index} = evt.currentTarget.dataset
        this.setData({
            [`content.commentList[${index}].is_like`]: !this.data.content.commentList[index].is_like
        })
    },
    likeComment2(evt) {
        const { index } = evt.currentTarget.dataset
        this.setData({
            [`content.artComment[${index}].is_like`]: !this.data.content.artComment[index].is_like
        })
    },
    // 查看大图
    preImg(evt) {
        let { current, imgs } = evt.currentTarget.dataset;
        wx.previewImage({
            urls: imgs.map(o => {
                return o
            }),
            current
        })
    },
})