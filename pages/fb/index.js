const qiniuUploader = require("../../utils/qiniu/qiniuUploader");
const app = getApp()


Page({
  data: {
    current: 1,
    audioList: [
      { name: "歌曲1", src: "http://ws.stream.qqmusic.qq.com/M500001VfvsJ21xFqb.mp3?guid=ffffffff82def4af4b12b3cd9337d5e7&uin=346897220&vkey=6292F51E1E384E06DCBDC9AB7C49FD713D632D313AC4858BACB8DDD29067D3C601481D36E62053BF8DFEAF74C0A5CCFADD6471160CAF3E6A&fromtag=46" },
      { name: "歌曲2", src: "http://ws.stream.qqmusic.qq.com/M500001VfvsJ21xFqb.mp3?guid=ffffffff82def4af4b12b3cd9337d5e7&uin=346897220&vkey=6292F51E1E384E06DCBDC9AB7C49FD713D632D313AC4858BACB8DDD29067D3C601481D36E62053BF8DFEAF74C0A5CCFADD6471160CAF3E6A&fromtag=46" }
    ],
    audioNum:null,
    isModal: false,
    imgObj: {
      imgList:[],
      title:'',
      artType:'',
      typeList: null,
      location: '',
      textContent: '',
      modelType:'0'
    },//图片表单数据
    videoObj: {
      videoList: [],
      title: '',
      artType: '',
      typeList: null,
      location: '',
      textContent: '',
      modelType: '1'
    },//视频表单数据
    textObj: {
      title: '',
      artType: '',
      typeList: null,
      location: '',
      textContent: '',
      modelType: '2',
      backMusic:null
    },//文字表单数据
    post: {}//最终提交的表单数据
  },
  typeChange(evt) {
    const { model } = evt.currentTarget.dataset
    if(model == '1'){//图片
      var typeKey = this.data.imgObj.typeList[evt.detail.value].k
      this.setData({
        'imgObj.artType': typeKey,
      })
    } else if (model == '2'){//视频
      var typeKey = this.data.videoObj.typeList[evt.detail.value].k
      this.setData({
        'videoObj.artType': typeKey,
      })
    }else{
      var typeKey = this.data.videoObj.typeList[evt.detail.value].k
      this.setData({
        'textObj.artType': typeKey,
      })
    }
  },
  openMap(evt) {
    wx.chooseLocation({
      success: res => {
        const { model } = evt.currentTarget.dataset
        if (model == '1') {//图片
          this.setData({
            'imgObj.location': res.address
          })
        } else if (model == '2'){//视频
          this.setData({
            'videoObj.location': res.address
          })
        }else{
          this.setData({
            'textObj.location': res.address
          })
        }
      },
    })
  },

  continueEdit(evt) {
    if (!this.data.modalContent) {
      return
    }
    wx.showModal({
      title: '提示',
      content: '确定继续编写未完成的文章吗？',
      cancelText: '忽略',
      confirmText: '继续',
      success: res => {
        if (res.confirm) {
          this.setData({
            [`post[${this.data.current}].text`]: this.data.modalContent
          })
          this.closeModal()

        } else if (res.cancel) {
          wx.removeStorage({
            key: 'lastText',
            success: function (res) {
              console.log(res.data)
            }
          })
          this.closeModal()
        }
      }
    })
  },
  closeModal() {
    this.setData({
      isModal: false,
      isAudio: false
    })
    // wx.stopBackgroundAudio()
  },
  showModal() {
    this.setData({
      isModal: true
    })
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    wx.getSystemInfo({
      success: res => {
        this.setData({
          imgSize: Math.floor((res.windowWidth - 40) / 3)
        })
      },
    })
    var that = this;
    wx.request({
      url: app.apiUrl + '/api/getArtTypeAndLevelList',
      success: function (res) {
        that.setData({
          'imgObj.typeList': res.data.data.typeList,
          'videoObj.typeList': res.data.data.typeList,
          'textObj.typeList': res.data.data.typeList
        })
      },
      fail: function (error) {
        console.error('获取艺术类别和艺术等级失败...: ' + error);
      }
    })
    //判断是否已认证
    if (app.user.artLevel === undefined || app.user.artLevel==null || app.user.artLevel==''){
      wx.showModal({
        title: '温馨提示',
        content: '只有通过认证的艺术家才能发布作品',
        confirmText: '立即认证',
        cancelText: '返回',
        success: function (res) {
          if (res.confirm) {
            //跳转到 tabBar个人中心页
            wx.reLaunch({
              url: '../mine/mine'
            })
          } else if (res.cancel) {
            wx.reLaunch({
              url: '../home/home'
            })
          }
        }
      })

    }
  },
  setCurrent(evt) {
    const { current } = evt.currentTarget.dataset
    this.setData({
      current
    })
    if (current == 2) {
      wx.stopBackgroundAudio();
    }
    if (current == 3 && (!this.data.post[3] || !this.data.post[3].text)) {
      //获取缓存
      wx.getStorage({
        key: 'lastText',
        success: res => {
          if (!res.data.trim()) {
            return
          }
          this.setData({
            modalContent: res.data
          })
          this.showModal()
        }
      })
    }
    // getApp().fbCurrent = evt.currentTarget.dataset.current
  },
  chooseImg() {
    initQiniu();
    wx.chooseImage({
      count:9,
      success: res => {
        wx.showLoading({
          title: '上传中...',
        })
        console.log(res.tempFilePaths)
       
        for (var i = 0; i < res.tempFilePaths.length;i++){
          var filePath = res.tempFilePaths[i];
          // 交给七牛上传
          qiniuUploader.upload(filePath, (res) => {
            var imgList = this.data.imgObj.imgList
            imgList.push(res.imageURL)
            this.setData({
              'imgObj.imgList': imgList,
            })
          }, (error) => {
            console.error('error: ' + JSON.stringify(error));
          });
        }
      },
      complete: function(){
        wx.hideLoading()
      }
    })
  },
  chooseVideo() {
    initQiniu();
    wx.chooseVideo({
      success: res => {
        wx.showLoading({
          title: '上传中...',
        })
        var filePath = res.tempFilePath;
        qiniuUploader.upload(filePath, (res) => {
          var videoList = this.data.videoObj.videoList
          videoList.push(res.imageURL)
          this.setData({
            'videoObj.videoList': videoList,
          })
          wx.hideLoading()
        }, (error) => {
          console.error('error: ' + JSON.stringify(error));
          wx.hideLoading()
        })
      }
    })
  },
  chooseAudio() {
    this.setData({
      isAudio: true
    })
  },
  clickAudio(evt) {
    const { index } = evt.currentTarget.dataset
    var that = this
    this.setData({
      "textObj.backMusic": that.data.audioList[index].src,
      "audioNum": index
    })
    const { src } = this.data.audioList[index]
    src && wx.playBackgroundAudio({
      dataUrl: src
    })
    this.setData({
      dataUrl: this.data.audioList[index]
    })
    !src && wx.stopBackgroundAudio()

    console.log(this.data.textObj);
  },
  onInput(evt) {
    const { model } = evt.currentTarget.dataset
    if (model == '1') {//图片
      this.setData({
        'imgObj.title': evt.detail.value,
      })
    } else if (model == '2') {//视频
      this.setData({
        'videoObj.title': evt.detail.value,
      })
    }else{
      this.setData({
        'textObj.title': evt.detail.value,
      })
    }
  },
  onText(evt) {
    const { model } = evt.currentTarget.dataset
    if (model == '1') {//图片
      this.setData({
        'imgObj.textContent': evt.detail.value,
      })
    } else if (model == '2') {//视频
      this.setData({
        'videoObj.textContent': evt.detail.value,
      })
    }else{
      this.setData({
        'textObj.textContent': evt.detail.value,
      })
    }
  },
  submit() {
    var currentPage = this.data.current
    var postData = null
    if (currentPage==1){//图片
      postData = this.data.imgObj
      if (postData.imgList===undefined || postData.imgList.length==0) {
        getApp().wxToast.warn("请选择图片")
        return
      }
    } else if (currentPage == 2){//视频
      postData = this.data.videoObj
      if (postData.videoList === undefined || postData.videoList.length == 0) {
        getApp().wxToast.warn("请选择视频")
        return
      }
    } else if (currentPage == 3) {//文字
      postData = this.data.textObj
      if (postData.textContent === undefined || postData.textContent.length == 0) {
        getApp().wxToast.warn("随便写点什么吧")
        return
      }
    }else{
      return
    }
    if (!postData) {
      getApp().wxToast.warn("请输入标题")
      return
    }
    if (!postData.title) {
      getApp().wxToast.warn("请输入标题")
      return
    }
    if (!postData.artType) {
      getApp().wxToast.warn("请选择艺术分类")
      return
    }
    // if (!postData.location) {
    //   getApp().wxToast.warn("请选择位置")
    //   return
    // }
    wx.showLoading({
      title: '正在提交.....',
    })
    postData.openId = app.openId
    postData.orgId = app.orgId
    console.log(JSON.stringify(postData))
    //提交认证表单
    wx.request({
      url: app.apiUrl + '/api/saveArtworks',
      header:{
        'content-type': 'application/json'
      },
      data: JSON.stringify(postData),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          //发布完成后清除缓存
          try {
            wx.removeStorageSync('lastText_' + currentPage)
          } catch (e) {
            console.log("清除缓存lastText_" + currentPage + "异常")
          }
          //跳转到 tabBar个人中心页
          wx.reLaunch({
            url: '../mine/mine'
          })

        } else {
          app.wxToast.error(res.data.msg);
        }

      },
      fail: function (error) {
        console.error('qiniu UploadToken is null, please check the init config or networking: ' + error);
      },
      complete: function () {
        wx.hideLoading()
      }
    })
  },

  saveCache(evt) {
    const { model } = evt.currentTarget.dataset
    wx.setStorage({
      key: 'lastText_' + model,
      data: evt.detail.value,
    })
  },
  imgClick(evt) {
    wx.showActionSheet({
      itemList: ['预览', '设为封面', '删除'],
      success: res => {
        const { index, src } = evt.currentTarget.dataset
        if (res.tapIndex == 0) {
          wx.previewImage({
            urls: [src],
          })
        }
        if (res.tapIndex == 1) {
          const { imgList } = this.data.imgObj
          imgList.unshift(imgList.splice(index, 1));
          this.setData({ 'imgObj.imgList': imgList })
        }
        if (res.tapIndex == 2) {
          const { imgList } = this.data.imgObj
          imgList.splice(index, 1)
          this.setData({ 'imgObj.imgList': imgList })
        }
      }
    })
  }
})
// 初始化七牛相关参数
function initQiniu() {
  var options = {
    region: 'ECN', // 华区
    uptokenURL: app.apiUrl + '/api/getUploadToken',
    domain: 'http://p3mjvv81y.bkt.clouddn.com',
    shouldUseQiniuFileName: false
  };
  qiniuUploader.init(options);
}