const app = getApp()
Page({
    /**
     * 页面的初始数据
     */
    data: {
      detailObj:null,
      current: 1,
      commentOj:{   //评论
        artWorksId:'',
        content:'',
        parent:null,
        openId:'',
        orgId:''
      },
      contentInput:'' //评论输入框默认为空
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
      //初始化评论作品ID
      that.setData({
        'commentOj.artWorksId': option.id,
        'commentOj.openId': app.openId,
        'commentOj.orgId': app.orgId
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
        console.log(evt)
        console.log(evt.currentTarget.dataset)
        console.log(evt.currentTarget.dataset.id)
        // this.setData({
        //     "content.is_keep": !this.data.content.is_keep
        // })
        var obj = {}
        obj.openId = ''
        id = ''
        wx.request({
          url: that.apiUrl + '/api/login',
          data: JSON.stringify(userObj),
          dataType: 'json',
          method: 'POST',
          success: function (res) {
            if (res.data.code == '0') {
              console.log('登录成功')
            } else {
              console.log('登录失败')
            }

          },
          fail: function (error) {
            console.error(' 登录异常: ' + error);
          },
          complete: function () {
            wx.hideLoading()
          }
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
    //评论
    onInput: function (evt) {
      var val = evt.detail.value;
      this.setData({
        'commentOj.content': val
      })

    },
    //提交
    submitBtn(){
      console.log(this.data.detailObj.commentList)
      var commentList = this.data.detailObj.commentList || []
      var newComment = {}
      newComment.photo = app.user.avatarUrl
      newComment.nickName = app.user.nickName
      newComment.artType = app.user.artType
      newComment.artLevel = app.user.artLevel
      newComment.dzNum = 1
      newComment.content = this.data.commentOj.content
      newComment.createDate = '刚刚'
      commentList.push(newComment)

      this.setData({
        'detailObj.commentList': commentList
      })
      //保存评论
      var that = this
      wx.request({
        url: app.apiUrl + '/api/saveComment',
        data: JSON.stringify(that.data.commentOj),
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
        complete: function () {}
      })
      //清除评论内容
      this.setData({
        contentInput:''
      })
    }
})