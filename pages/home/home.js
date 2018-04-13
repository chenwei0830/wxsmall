var util = require('../../utils/util.js');  
let catScrollLeft = 0
const app = getApp()
Page({
  data: {
    current: -1,
    categoryList:[],
    contentObj:[]   //已加载内容
  },
  onLoad: function (options) {
    getApp().editTabBar()
    wx.showLoading({
      title: '正在加载.....',
    })
    var that = this
    wx.request({
      url: app.apiUrl + '/api/getCategoryList',
      success: function (res) {
        that.setData({
          categoryList: res.data.data
        })
        that.clickCategory({
          currentTarget: {
            dataset: {
              current: 0
            }
          }
        })
      },
      fail: function (error) {
        console.error('获取分类失败...: ' + error);
      }, complete: function () {
        wx.hideLoading()
      }
    })
  },
  clickCategory(evt) {
    const { current } = evt.currentTarget.dataset
    if (this.data.current == current) {
      return
    }
    this.setData({
      current,
    })
    this.loadData()
    if (wx.createSelectorQuery) {
      const query = wx.createSelectorQuery()
      // query.select("#cat" + this.data.current).boundingClientRect()
      query.selectViewport().scrollOffset
      query.exec(res => {
        let scrollX = res[0].scrollLeft
        this.setData({
          tag: {
            left: res[0].left + catScrollLeft + 15,
            width: res[0].width - 30,
          },
          scrollX
        })
      })
    }
  },
  onCatScroll(e) {
    catScrollLeft = e.detail.scrollLeft
  },
  //加载数据
  loadData() {
    const { current } = this.data
    var category = this.data.categoryList[current]
    var curentObj = this.data.contentObj[current]
    if (curentObj === undefined){
      var obj = {}
      obj.loading = false
      obj.date = util.formatTime(new Date());
      obj.artTypeParam = category.id
      obj.beenBottom = false
      obj.beenTop = false
      obj.artWorksList = []
      this.setData({
        [`contentObj[${current}]`]: obj
      })
    }
    if (this.data.contentObj[current].loading) {
      return
    }
    //分页加载
    wx.showLoading({
      title: '正在加载.....',
    })
    var that = this
    wx.request({
      url: app.apiUrl + '/api/v1/listHome/' + category.id,
      // header: {
      //   'content-type': 'application/json'
      // },
      // data: JSON.stringify(that.data.contentObj[current]),
      // dataType: 'json',
      // method: 'POST',
      success: function (res) {
        if(res.data.code=='0'){
          var artWorksList = res.data.data
          if (artWorksList!=undefined && artWorksList.length>0){
            var currentArwWorksList = that.data.contentObj[current].artWorksList.concat(artWorksList)
            that.setData({
              [`contentObj[${current}].artWorksList`]: currentArwWorksList,
              [`contentObj[${current}].date`]: artWorksList[artWorksList.length-1].createDate
            })
          } 
        }
      },
      fail: function (error) {
        console.error('获取数据失败...: ' + error);
      }, complete: function () {
        wx.hideLoading()
      }
    })

    wx.stopPullDownRefresh()
    wx.hideLoading()
  },

  onReachBottom() {
    //上拉加载
    this.loadData()
  },
  onPullDownRefresh() {
    //下拉刷新
    const { current } = this.data
    const cate = this.data.category[current]
    cate.total = 2
    cate.page = 1
    cate.content = []
    this.setData({
      [`category[${current}]`]: cate
    })
    this.loadData()
  },

  // onReady() {
  //   console.log(this.data.categoryList)

    
  // },
  //收藏
  keepClick(evt) {
    //此处请求接口
    const { pos } = evt.currentTarget.dataset
    const { is_keep } = this.data.category[pos[0]].content[pos[1]]

    var collectObj = {}
    collectObj.openId = 'o7tbx0KPXyVui_VUg9YgK4UauIWc'
    collectObj.artWorksId = '828d882ea22347c6801c375c0d6b1509'
    collectObj.orgId = app.orgId
    if (is_keep === 0 || !is_keep){//收藏
      collectObj.type = 1
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
          console.error(' 收藏异常: ' + error);
        },
        complete: function () {
          wx.hideLoading()
        }
      })
    } else {//取消收藏
      collectObj.type = 0
      wx.request({
        url: app.apiUrl + '/api/collectArtworks',
        data: JSON.stringify(collectObj),
        dataType: 'json',
        method: 'POST',
        success: function (res) {
          if (res.data.code == '0') {
            console.log('取消收藏成功')
          } else {
            console.log('取消收藏失败')
          }

        },
        fail: function (error) {
          console.error('取消收藏异常: ' + error);
        },
        complete: function () {
          wx.hideLoading()
        }
      })
    }
    this.setData({
      [`category[${pos[0]}].content[${pos[1]}].is_keep`]: !is_keep
    })
  },
  commentClick(evt) {
    //此处请求评论列表的接口
    const { pos } = evt.currentTarget.dataset
    const { is_comment } = this.data.category[pos[0]].content[pos[1]]
    this.setData({
      [`category[${pos[0]}].content[${pos[1]}].is_comment`]: !is_comment
    })
  },

  likeComment(evt) {
    const [x, y, z] = evt.currentTarget.dataset.pos
    console.log(x, y, z)
    this.setData({
      [`category[${x}].content[${y}].commentList[${z}].isLike`]: !this.data.category[x].content[y].commentList[z].isLike
    })
  },
  //点赞
  likeClick(evt) {
    //此处请求接口
    const { pos } = evt.currentTarget.dataset
    const { is_like } = this.data.category[pos[0]].content[pos[1]]
    this.setData({
      [`category[${pos[0]}].content[${pos[1]}].is_like`]: !is_like
    })
  },
  onShareAppMessage() {
    return {
      title: "西南艺术馆",//分享名称
    }
  }
})