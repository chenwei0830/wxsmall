module.exports = {
    error(title) {
        wx.showToast({
            title,
            image: "/assets/icon/toast-error.png"
        })
    },

    warn(title) {
        wx.showToast({
            title,
            image: "/assets/icon/toast-warn.png"
        })
    },

    success(title) {
      wx.showToast({
        title,
        image: "/assets/icon/toast-success.png"
      })
    }
}