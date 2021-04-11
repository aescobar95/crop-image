import './App.css';
import "bootstrap/dist/css/bootstrap.min.css";

import UploadImages from "./components/image-upload";

function App() {
  return (
    <div className="container">
      <h4>Upload and process image with OpenCV</h4>

      <div className="content">
        <UploadImages />
      </div>
    </div>
  );
}

export default App;
