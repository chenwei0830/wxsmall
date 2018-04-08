// pages/home/home.js
let catScrollLeft = 0
const app = getApp()
Page({
  data: {
    current: -1,
    category: [
      {
        id: 0,
        name: "推荐",
      },
      {
        id: 1,
        name: "关注",
      },
      {
        id: 2,
        name: "摄影"
      },
      {
        id: 3,
        name: "美术"
      },
      {
        id: 4,
        name: "书法"
      },
      {
        id: 5,
        name: "文学"
      },
      {
        id: 6,
        name: "文学"
      },
      {
        id: 7,
        name: "文学"
      },
    ],
  },
  onLoad: function (options) {
    getApp().editTabBar();
  },
  clickCategory(evt) {
    const { current } = evt.currentTarget.dataset
    if (this.data.current == current) {
      return
    }
    this.setData({
      current,
    })
    const { content = [] } = this.data.category[current]
    content.length == 0 && this.loadData()
    if (wx.createSelectorQuery) {
      const query = wx.createSelectorQuery()
      //query.select("#cat" + this.data.current).boundingClientRect()
      query.selectViewport().scrollOffset
      query.exec(res => {
        console.log(res[0].scrollLeft)
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

  loadData() {
    const { current } = this.data
    console.log('current--'+current)
    let { page = 1, total = 2, loading = false, content = [], id } = this.data.category[current]

    if (page > total || loading) {
      return
    }
    this.setData({
      [`category[${current}].loading`]: true
    })
    //分页加载
    //此处请求接口
    wx.showLoading({
      title: '加载中...',
    })
    console.log(content.length)
    page++
    total = 10
    const arr = []
    for (let i = 0; i < 10; i++) {
      arr.push({
        id: 1,
        avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
        name: 'SHERRY & MOLLY',
        label: '摄影',
        grade: 2,
        address: '四川省成都市高新区腾讯大厦哈哈哈哈或',
        date: '09-13',
        time: '09:42',
        title: '简约而不简单',
        image: [
          'http://oj1itelvn.bkt.clouddn.com/hhh.jpg',
          'http://oj1itelvn.bkt.clouddn.com/art/artist-bg.png'
        ],
        is_keep: 0,
        commentList: [
          {
            avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
            name: '像个杂货铺',
            label: '画家',
            detail: '索拉卡的叫法思考四大件发生考虑到，桑德菲杰sd卡家乐福圣诞节放假撒快递放假。',
            date: '01/05',
            time: '10:55',
            is_like: 0,
            grade: 0
          }
        ],
        is_comment: 0,
        is_like: 0
      },
        {
          id: 2,
          avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
          name: 'SHERRY & MOLLY',
          label: '摄影',
          grade: 1,
          address: '四川省成都市高新区腾讯大厦哈哈哈哈或',
          date: '09-13',
          time: '09:42',
          title: '阿道夫洒洒地发生地方',
          image: '',
          video: 'http://oj1itelvn.bkt.clouddn.com/art/test-mp4.mp4',
          is_keep: 0,
          commentList: [
            {
              avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
              name: '像个杂货铺',
              label: '画家',
              detail: '索拉卡的叫法思考四大件发生考虑到，桑德菲杰sd卡家乐福圣诞节放假撒快递放假。',
              date: '01/05',
              time: '10:55',
              is_like: 0,
              grade: 1
            }
          ],
          is_comment: 0,
          is_like: 0
        },
        {
          id: 3,
          avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
          name: 'SHERRY & MOLLY',
          label: '哈哈哈',
          grade: 3,
          address: '撒的发生地方撒旦',
          date: '09-13',
          time: '09:42',
          title: '阿道夫洒洒地发生地方阿拉克圣诞节发送快递费啊刷卡机地方哈撒娇快递发货，阿克苏鹿鼎记发送旅客的撒抵抗力房间阿斯利康地方。的减肥哈温和复苏的护发素的角度看健康撒地方和圣诞节，阿斯顿空间粉红色的尽快发哈萨克的减肥。',
          image: '',
          video: '',
          is_keep: 0,
          commentList: [
            {
              avatar: 'http://oj1itelvn.bkt.clouddn.com/timg.jpg',
              name: '像个杂货铺',
              label: '画家',
              detail: '索拉卡的叫法思考四大件发生考虑到，桑德菲杰sd卡家乐福圣诞节放假撒快递放假。',
              date: '01/05',
              time: '10:55',
              is_like: 0,
              grade: 1
            }
          ],
          is_comment: 0,
          is_like: 0
        }
      )
    }
    content.push(...arr)
    console.log(content.length)
    loading = false
    wx.stopPullDownRefresh()
    wx.hideLoading()
    this.setData({
      [`category[${current}]`]: Object.assign(this.data.category[current], {
        page, total, loading, content
      })
    })
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

  onReady() {
    this.clickCategory({
      currentTarget: {
        dataset: {
          current: 0
        }
      }
    })
  },
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
      title: "成都xx公司",//分享名称
    }
  }
})