
const qiniuUploader = require("../../../utils/qiniu/qiniuUploader");
const app = getApp()
const time = 60
Page({

  /**
   * 页面的初始数据
   */
  data: {
    codeTime: time,
    waitTime: time,
    typeList: [],//艺术分类
    levelList: [],//艺术级别
    post: {},//表单
    checked: true,
    sslList: []
  },
  onLoad() {
    //计算图片展示大小
    var that = this;
    wx.getSystemInfo({
      success: res => {
        this.setData({
          imgSize: Math.floor((res.windowWidth - 40) / 3)
        })
      },
    }),
      wx.request({
        url: app.apiUrl + '/api/getArtTypeAndLevelList',
        success: function (res) {
          that.setData({
            typeList: res.data.data.typeList,
            levelList: res.data.data.levelList,
          });
        },
        fail: function (error) {
          console.error('获取艺术类别和艺术等级失败...: ' + error);
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
  typeChange(evt) {
    var typeKey = this.data.typeList[evt.detail.value].k
    const { key } = evt.currentTarget.dataset
    this.setData({
      [`post.${key}`]: typeKey,
    })
  },
  levelChange(evt) {
    var levelKey = this.data.levelList[evt.detail.value].k
    const { key } = evt.currentTarget.dataset
    this.setData({
      [`post.${key}`]: levelKey,
    })
  },
  addSSL() {
    initQiniu();
    wx.chooseImage({
      success: res => {
        var filePath = res.tempFilePaths[0];
        // 交给七牛上传
        qiniuUploader.upload(filePath, (res) => {
          const { sslList = [] } = this.data
          sslList.push(res.imageURL)
          this.setData({ sslList })
        }, (error) => {
          console.error('error: ' + JSON.stringify(error));
        });
      },
    })
  },
  getCode() {
    if (!this.data.post.phone) {
      app.wxToast.warn('请输入手机号码');
      return;
    }
    if (!this.data.post.phone.match(/^1[3|4|5|7|8][0-9]\d{4,8}$/)) {
      app.wxToast.warn('请输入正确手机号');
      return;
    }
    let { codeTime, waitTime } = this.data
    if (codeTime != waitTime) {
      return
    }
    codeTime--
    this.setData({ codeTime })
    //获取验证码接口
    console.log("get-code")
    //倒计时
    var tid = setInterval(() => {
      codeTime--
      if (codeTime <= 0) {
        codeTime = waitTime
        clearInterval(tid)
      }
      this.setData({ codeTime })
    }, 1000)


  },
  check() {
    this.setData({
      checked: !this.data.checked
    })
  },
  imgClick(evt) {
    wx.showActionSheet({
      itemList: ['预览', '删除'],
      success: res => {
        const { index, src } = evt.currentTarget.dataset
        if (res.tapIndex == 0) {
          wx.previewImage({
            urls: [src],
          })
        }

        if (res.tapIndex == 1) {
          const { imgList } = this.data
          imgList.splice(index, 1)
          this.setData({ imgList })
        }
      }
    })
  },

  submit: function () {
    const { post } = this.data
    if (!post.name) {
      app.wxToast.warn('请输入姓名');
      return;
    }
    if (!post.card) {
      app.wxToast.warn('请输入身份证号码');
      return;
    }
    if (!post.card.match(/^\d{6}(18|19|20)?\d{2}(0[1-9]|1[12])(0[1-9]|[12]\d|3[01])\d{3}(\d|X)$/i)) {
      app.wxToast.warn('请输入正确的身份证号码');
      return;
    }
    if (!post.phone) {
      app.wxToast.warn('请输入联系电话');
      return;
    }
    if (!post.phone.match(/^1[3|4|5|7|8][0-9]\d{4,8}$/)) {
      app.wxToast.warn('请输入正确手机号');
      return;
    }
    if (!post.code) {
      app.wxToast.warn('请输入短信验证码');
      return;
    }
    if (post.artType === undefined) {
      app.wxToast.warn('请选择您的专业分类');
      return;
    }
    if (post.artLevel === undefined) {
      app.wxToast.warn('请选择您的艺术级别');
      return;
    }
    if (!this.data.sslList.length) {
      app.wxToast.warn('请添加证书');
      return;
    }
    if (this.data.checked == false) {
      app.wxToast.warn('请查看合约条款');
      return;
    }

    var obj = {};
    obj.openId = app.openId
    obj.name = post.name
    obj.idCard = post.card
    obj.phone = post.phone
    obj.artType = post.artType
    obj.artLevel = post.artLevel
    obj.status = '0'
    obj.feePayNo = 'pay_no'
    obj.feePayStatus = '0'
    obj.rList = this.data.sslList
    obj.orgId = app.orgId
    wx.showLoading({
      title: '正在提交...',
    })
    //提交认证表单
    wx.request({
      url: app.apiUrl + '/api/auth',
      data: JSON.stringify(obj),
      dataType: 'json',
      method: 'POST',
      success: function (res) {
        if (res.data.code == '0') {
          //跳转到 tabBar个人中心页
          wx.reLaunch({
            url: '../../mine/mine'
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

})
// 初始化七牛相关参数
function initQiniu() {
  var options = {
    region: 'ECN', // 华区
    uptokenURL: app.apiUrl + '/api/getUploadToken',
    //uptoken: 'ZB5LeFm0VbqTWGNLoV6YGSqRGq0ljk38wRsTevT7:dpBDZ3lch7lmSfMED1dCkhObjs4=:eyJzY29wZSI6InNvdXJ0aGFydHN5cyIsImRlYWRsaW5lIjoxNTIwNjU5OTkyfQ==',
    domain: 'http://p3mjvv81y.bkt.clouddn.com',
    shouldUseQiniuFileName: false
  };
  qiniuUploader.init(options);
}