const app = getApp()
Page({
  /**
   * 页面的初始数据
   */
  data: {
    detailObj: null,
    currentArtWorksId:'',  //当前作品ID
    contentInput: '', //评论输入框默认为空
    contentPL:'',     //评论内容
    parentPLId:null,  //父类评论ID（针对回复评论使用）
  },
  onLoad: function (option) {
    //获取作品详情
    var that = this
    wx.request({
      url: app.apiUrl + '/api/getArtWorksDetail',
      data: { id: option.id, openId: app.openId },
      success: function (e) {
        if (e.data.code == '0') {
          that.setData({
            detailObj: e.data.data
          })
        }
        console.log(that.data.detailObj)
      }
    })
    //初始化评论作品ID
    that.setData({
      'currentArtWorksId': option.id
    })
  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {
    return {
      title: this.data.detailObj.title,//分享名称
    }
  },
  like(evt) {
    this.setData({
      "detailObj.hasDz": this.data.detailObj.hasDz > 0 ? 0 : 1,
      "detailObj.dzNum": this.data.detailObj.hasDz > 0 ? this.data.detailObj.dzNum - 1 : this.data.detailObj.dzNum + 1
    })
    //保存或取消点赞
    var dzObj = {}
    dzObj.openId = app.openId
    dzObj.orgId = app.orgId
    dzObj.targetId = this.data.currentArtWorksId
    dzObj.type = '0' //0-作品 1-评论
    dzObj.delFlag = this.data.detailObj.hasDz > 0 ?'0':'1'
    wx.request({
      url: app.apiUrl + '/api/targetDz',
      data: JSON.stringify(dzObj),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          console.log('点赞或取消点赞成功')
        } else {
          console.log('点赞或取消点赞失败')
        }
      },
      fail: function (error) {
        console.error(' 系统异常: ' + error);
      },
      complete: function () { }
    })
  },
  collectBtn(evt) {
    this.setData({
      "detailObj.hasCollected": this.data.detailObj.hasCollected > 0 ? 0 : 1
    })
    //保存或取消关注
    var collectObj = {}
    collectObj.openId = app.openId
    collectObj.orgId = app.orgId
    collectObj.artWorksId = this.data.currentArtWorksId
    collectObj.type = this.data.detailObj.hasCollected
    wx.request({
      url: app.apiUrl + '/api/collectArtworks',
      data: JSON.stringify(collectObj),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          console.log('收藏成功')
        } else {
          console.log('收藏失败')
        }

      },
      fail: function (error) {
        console.error(' 系统异常: ' + error);
      },
      complete: function () {}
    })
  },
  likeComment(evt) {
    var index = evt.currentTarget.dataset.index
    this.setData({
      [`detailObj.commentList[${index}].hasDz`]: this.data.detailObj.commentList[index].hasDz > 0 ? 0 : 1,
      [`detailObj.commentList[${index}].dzNum`]: this.data.detailObj.commentList[index].hasDz > 0 ? this.data.detailObj.commentList[index].dzNum - 1 : this.data.detailObj.commentList[index].dzNum + 1
    })
    //保存或取消点赞
    //保存或取消点赞
    var dzObj = {}
    dzObj.openId = app.openId
    dzObj.orgId = app.orgId
    dzObj.targetId = this.data.detailObj.commentList[index].id
    dzObj.type = '1' //0-作品 1-评论
    dzObj.delFlag = this.data.detailObj.commentList[index].hasDz>0 ? '0' : '1'
    wx.request({
      url: app.apiUrl + '/api/targetDz',
      data: JSON.stringify(dzObj),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          console.log('点赞或取消点赞成功')
        } else {
          console.log('点赞或取消点赞失败')
        }
      },
      fail: function (error) {
        console.error(' 系统异常: ' + error);
      },
      complete: function () { }
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
  //评论
  onInput: function (evt) {
    var val = evt.detail.value;
    this.setData({
      'contentPL': val
    })
  },
  //提交
  submitBtn() {
    var commentList = this.data.detailObj.commentList || []
    var newComment = {}
    newComment.photo = app.user.avatarUrl
    newComment.nickName = app.user.nickName
    newComment.artType = app.user.artType
    newComment.artLevel = app.user.artLevel
    newComment.dzNum = 0
    newComment.content = this.data.contentPL
    newComment.createDate = '刚刚'
    commentList.push(newComment)

    this.setData({
      'detailObj.commentList': commentList,
      "detailObj.plNum": this.data.detailObj.plNum + 1,
    })
    //保存评论
    var cObj = {}
    cObj.openId = app.openId
    cObj.orgId = app.orgId
    cObj.artWorksId = this.data.currentArtWorksId
    cObj.content = this.data.contentPL
    wx.request({
      url: app.apiUrl + '/api/saveComment',
      data: JSON.stringify(cObj),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          console.log('评论成功')
        } else {
          app.wxToast.warn('评论失败');
        }
      },
      fail: function (error) {
        app.wxToast.warn('网络异常');
      },
      complete: function () { }
    })
    //清除评论内容
    this.setData({
      contentInput: ''
    })
  }
})