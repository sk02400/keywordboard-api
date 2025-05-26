function getFaviconUrl(url) {
  try {
    const urlObj = new URL(url);
    return `${urlObj.origin}/favicon.ico`;
  } catch (e) {
    return null;
  }
}

module.exports = { getFaviconUrl };
